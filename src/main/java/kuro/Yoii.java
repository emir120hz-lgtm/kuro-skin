package kuro;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Yoii: baska oyunculari sadece bizim ekranimizda "karakter" olarak gosterir.
 *
 * Karakter = skin (64x64 PNG) + istege bagli 3B ek geometri (kendi JSON model formatimiz).
 * Ek geometri vanilla kemiklere (head/body/arms/legs) baglanan kutulardir; kendi dokusunu
 * kullanabilir ya da skinin piksellerini gerer ("texture":"skin"). Format icin YOII_MODEL.md.
 * Sunucuya hicbir sey gitmez.
 */
public final class Yoii {
   public static final String[] BUILTIN = {"skin_1", "skin_2", "skin_3", "skin_4", "skin_5"};

   /** Bir 3B ek geometri seti. tex == null ise oyuncunun skin dokusu kullanilir. */
   public static final class Extras {
      Identifier tex;
      final List<String> bones = new ArrayList<>();
      final List<ModelPart> parts = new ArrayList<>();
   }

   public static final class Char {
      final Identifier skin;
      final Extras model;

      Char(Identifier skin, Extras model) {
         this.skin = skin;
         this.model = model;
      }
   }

   private static final List<Char> POOL = new ArrayList<>();
   private static boolean loaded;
   private static int texCounter;
   private static Extras preset;

   /** setAngles sirasinda ayarlanir: su an cizilen model bir Yoii hedefi mi ve hangi karakter. */
   public static boolean extra;
   public static Char cur;

   // Skinin kendi piksellerini geren dahili 3B set: etek, bot, kol manseti (dokuya "skin" kullanir).
   private static final String PRESET = "{\"texture\":\"skin\",\"tex_w\":64,\"tex_h\":64,\"parts\":["
      + "{\"bone\":\"body\",\"cubes\":[{\"o\":[-4,8,-2],\"s\":[8,4,4],\"uv\":[16,24],\"d\":1.3}]},"
      + "{\"bone\":\"right_leg\",\"cubes\":[{\"o\":[-2,8,-2],\"s\":[4,4,4],\"uv\":[0,24],\"d\":0.5}]},"
      + "{\"bone\":\"left_leg\",\"cubes\":[{\"o\":[-2,8,-2],\"s\":[4,4,4],\"uv\":[16,56],\"d\":0.5}]},"
      + "{\"bone\":\"right_arm\",\"cubes\":[{\"o\":[-3,8,-2],\"s\":[4,4,4],\"uv\":[40,24],\"d\":0.6}]},"
      + "{\"bone\":\"left_arm\",\"cubes\":[{\"o\":[-1,8,-2],\"s\":[4,4,4],\"uv\":[32,56],\"d\":0.6}]}"
      + "]}";


   // skin_5 icin ozel 3B model: uzun saç (3 kademe), yan perçemler, topuz, çift katlı etek, uzun bot, geniş kol.
   // Tum parcalar skinin kendi piksellerini gerer (texture: skin).
   private static final String MODEL_SKIN5 = "{\"texture\":\"skin\",\"tex_w\":64,\"tex_h\":64,\"parts\":["
      + "{\"bone\":\"body\",\"cubes\":[{\"o\":[-4,-1,2.2],\"s\":[8,4,1],\"uv\":[14,7]}]},"
      + "{\"bone\":\"body\",\"cubes\":[{\"o\":[-4,3,2.4],\"s\":[8,4,1],\"uv\":[14,9],\"d\":0.1}]},"
      + "{\"bone\":\"body\",\"cubes\":[{\"o\":[-4,7,2.6],\"s\":[8,4,1],\"uv\":[14,11],\"d\":0.3}]},"
      + "{\"bone\":\"head\",\"rot\":[0,0,8],\"pivot\":[4.6,-6,0],\"cubes\":[{\"o\":[0,0,-1],\"s\":[2,9,2],\"uv\":[20,6]}]},"
      + "{\"bone\":\"head\",\"rot\":[0,0,-8],\"pivot\":[-4.6,-6,0],\"cubes\":[{\"o\":[-2,0,-1],\"s\":[2,9,2],\"uv\":[20,6]}]},"
      + "{\"bone\":\"head\",\"pivot\":[3.2,-9,0],\"cubes\":[{\"o\":[-1.5,-1.5,-1.5],\"s\":[3,3,3],\"uv\":[5,0],\"d\":0.4}]},"
      + "{\"bone\":\"head\",\"pivot\":[-3.2,-9,0],\"cubes\":[{\"o\":[-1.5,-1.5,-1.5],\"s\":[3,3,3],\"uv\":[5,0],\"d\":0.4}]},"
      + "{\"bone\":\"body\",\"cubes\":[{\"o\":[-4,8,-2],\"s\":[8,4,4],\"uv\":[16,24],\"d\":1.0}]},"
      + "{\"bone\":\"body\",\"cubes\":[{\"o\":[-4,10,-2],\"s\":[8,2,4],\"uv\":[16,26],\"d\":2.0}]},"
      + "{\"bone\":\"right_leg\",\"cubes\":[{\"o\":[-2,6,-2],\"s\":[4,6,4],\"uv\":[0,22],\"d\":0.6}]},"
      + "{\"bone\":\"left_leg\",\"cubes\":[{\"o\":[-2,6,-2],\"s\":[4,6,4],\"uv\":[16,54],\"d\":0.6}]},"
      + "{\"bone\":\"right_arm\",\"cubes\":[{\"o\":[-3,6,-2],\"s\":[4,6,4],\"uv\":[40,22],\"d\":0.9}]},"
      + "{\"bone\":\"left_arm\",\"cubes\":[{\"o\":[-1,6,-2],\"s\":[4,6,4],\"uv\":[32,54],\"d\":0.9}]}"
      + "]}";

   // Sac uzantisi icin ayri kucuk modeller (skinin kafa arkasi 24,8 pikselini gerer).
   private static ModelPart panel;
   private static ModelPart tailL;
   private static ModelPart tailR;

   private static float num(JsonArray a, int i, float def) {
      return a != null && a.size() > i ? a.get(i).getAsFloat() : def;
   }

   /** JSON modelden 3B ek geometri kurar. dir: ek doku dosyalarinin klasoru (null olabilir). */
   private static Extras parse(JsonObject o, Path dir) throws Exception {
      Extras ex = new Extras();
      String t = o.has("texture") ? o.get("texture").getAsString() : "skin";
      if (!t.equals("skin") && dir != null) {
         ex.tex = loadTexture(dir.resolve(t));
      }

      int tw = o.has("tex_w") ? o.get("tex_w").getAsInt() : 64;
      int th = o.has("tex_h") ? o.get("tex_h").getAsInt() : 64;
      ModelData md = new ModelData();
      ModelPartData root = md.getRoot();
      JsonArray arr = o.getAsJsonArray("parts");
      int n = 0;
      for (JsonElement pe : arr) {
         JsonObject p = pe.getAsJsonObject();
         ModelPartBuilder b = ModelPartBuilder.create();
         for (JsonElement ce : p.getAsJsonArray("cubes")) {
            JsonObject c = ce.getAsJsonObject();
            JsonArray po = c.getAsJsonArray("o");
            JsonArray ps = c.getAsJsonArray("s");
            JsonArray pu = c.getAsJsonArray("uv");
            float d = c.has("d") ? c.get("d").getAsFloat() : 0.0F;
            b.uv((int)num(pu, 0, 0), (int)num(pu, 1, 0))
               .cuboid(num(po, 0, 0), num(po, 1, 0), num(po, 2, 0), num(ps, 0, 1), num(ps, 1, 1), num(ps, 2, 1), new Dilation(d));
         }

         JsonArray pv = p.has("pivot") ? p.getAsJsonArray("pivot") : null;
         JsonArray rt = p.has("rot") ? p.getAsJsonArray("rot") : null;
         float k = (float)(Math.PI / 180.0);
         root.addChild("p" + n, b, ModelTransform.of(num(pv, 0, 0), num(pv, 1, 0), num(pv, 2, 0), num(rt, 0, 0) * k, num(rt, 1, 0) * k, num(rt, 2, 0) * k));
         ex.bones.add(p.get("bone").getAsString());
         n++;
      }

      ModelPart r = TexturedModelData.of(md, tw, th).createModel();
      for (int i = 0; i < n; i++) {
         ex.parts.add(r.getChild("p" + i));
      }

      return ex;
   }

   private static Identifier loadTexture(Path png) throws Exception {
      try (InputStream in = Files.newInputStream(png)) {
         NativeImage img = NativeImage.read(in);
         Identifier id = Identifier.of("kuro", "yoii_custom/" + (texCounter++));
         Kuro.mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
         return id;
      }
   }

   private static void load() {
      loaded = true;
      POOL.clear();
      texCounter = 0;
      try {
         preset = parse(JsonParser.parseString(PRESET).getAsJsonObject(), null);
      } catch (Throwable ignored) {
         preset = null;
      }

      for (String n : BUILTIN) {
         Extras bm = null;
         if (n.equals("skin_5")) {
            try {
               bm = parse(JsonParser.parseString(MODEL_SKIN5).getAsJsonObject(), null);
            } catch (Throwable ignored) {
            }
         }

         POOL.add(new Char(Identifier.of("kuro", "textures/yoii/" + n + ".png"), bm));
      }

      try {
         Path dir = FabricLoader.getInstance().getGameDir().resolve("kuro_yoii");
         Files.createDirectories(dir);
         List<Path> files = new ArrayList<>();
         try (var st = Files.list(dir)) {
            st.sorted().forEach(files::add);
         }

         for (Path p : files) {
            String name = p.toString().toLowerCase();
            try {
               if (name.endsWith(".png")) {
                  // "model_" onekli PNG'ler JSON modellerin dokusudur, tek basina skin sayilmaz
                  if (!p.getFileName().toString().toLowerCase().startsWith("model_")) {
                     POOL.add(new Char(loadTexture(p), null));
                  }
               } else if (name.endsWith(".json")) {
                  JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                  Extras ex = parse(o, dir);
                  Identifier skin = o.has("skin") ? loadTexture(dir.resolve(o.get("skin").getAsString()))
                     : Identifier.of("kuro", "textures/yoii/" + BUILTIN[0] + ".png");
                  POOL.add(new Char(skin, ex));
               }
            } catch (Throwable ignored) {
            }
         }
      } catch (Throwable ignored) {
      }
   }

   /** Klasoru yeniden okur (menudeki "Yenile" ayari). */
   public static void reload() {
      load();
   }

   public static int count() {
      if (!loaded) {
         load();
      }

      return POOL.size();
   }

   /** mode 0: secili karakter, 1: oyuncu basina rastgele (sabit), 2: sirayla hepsi (entity id'ye gore). */
   public static Char charFor(int entityId) {
      if (!loaded) {
         load();
      }

      int n = POOL.size();
      int mode = (int)Config.yoiiMode;
      int idx;
      if (mode == 0) {
         idx = Math.floorMod((int)Config.yoiiSkin, n);
      } else if (mode == 1) {
         idx = Math.floorMod(entityId * 0x9E3779B1, n);
      } else {
         idx = Math.floorMod(entityId, n);
      }

      return POOL.get(idx);
   }

   public static Identifier pick(int entityId) {
      return charFor(entityId).skin;
   }

   private static ModelPart bone(PlayerEntityModel m, String b) {
      return switch (b) {
         case "head" -> m.head;
         case "left_arm" -> m.leftArm;
         case "right_arm" -> m.rightArm;
         case "left_leg" -> m.leftLeg;
         case "right_leg" -> m.rightLeg;
         default -> m.body;
      };
   }

   private static void buildHair() {
      ModelData md = new ModelData();
      ModelPartData root = md.getRoot();
      root.addChild("panel", ModelPartBuilder.create().uv(14, 7).cuboid(-4.0F, -1.0F, 2.2F, 8.0F, 8.0F, 1.0F), ModelTransform.NONE);
      root.addChild("tl", ModelPartBuilder.create().uv(14, 7).cuboid(-4.0F, 0.0F, 0.0F, 8.0F, 8.0F, 1.0F), ModelTransform.pivot(5.4F, -6.0F, 2.5F));
      root.addChild("tr", ModelPartBuilder.create().uv(14, 7).cuboid(-4.0F, 0.0F, 0.0F, 8.0F, 8.0F, 1.0F), ModelTransform.pivot(-5.4F, -6.0F, 2.5F));
      ModelPart r = TexturedModelData.of(md, 64, 64).createModel();
      panel = r.getChild("panel");
      tailL = r.getChild("tl");
      tailR = r.getChild("tr");
   }

   public static void renderExtras(PlayerEntityModel m, MatrixStack ms, VertexConsumer vc, int light, int overlay, int color) {
      Char c = cur;
      Extras ex = c != null && c.model != null ? c.model : (Config.yoii3d >= 0.5F ? preset : null);
      if (ex != null) {
         VertexConsumer v = vc;
         if (ex.tex != null) {
            v = Kuro.mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayer.getEntityCutoutNoCull(ex.tex));
         }

         for (int i = 0; i < ex.parts.size(); i++) {
            ms.push();
            bone(m, ex.bones.get(i)).rotate(ms);
            ex.parts.get(i).render(ms, v, light, overlay, color);
            ms.pop();
         }
      }

      // Sac uzantisi: ozel modeli olmayan karakterlerde
      int mode = (int)Config.yoiiHair;
      if (mode <= 0 || (c != null && c.model != null)) {
         return;
      }

      if (panel == null) {
         buildHair();
      }

      float len = Config.yoiiHairLen;
      if (mode == 1) {
         panel.yScale = len;
         ms.push();
         m.body.rotate(ms);
         panel.render(ms, vc, light, overlay, color);
         ms.pop();
      } else {
         tailL.xScale = 0.4F;
         tailR.xScale = 0.4F;
         tailL.yScale = len;
         tailR.yScale = len;
         tailL.roll = -0.12F;
         tailR.roll = 0.12F;
         ms.push();
         m.head.rotate(ms);
         tailL.render(ms, vc, light, overlay, color);
         tailR.render(ms, vc, light, overlay, color);
         ms.pop();
      }
   }
}
