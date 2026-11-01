package kuro.mixin;

import kuro.Kuro;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Yoii: baska oyunculari (kendimiz haric) sadece bizim ekranimizda ozel, orijinal bir "anime kiz"
 * dokusuyla gosterir. Sunucuya hicbir sey gitmez, baska hicbir oyuncu etkilenmez. Doku tamamen
 * elle/kodla uretilmis, hicbir gercek oyun/anime karakterinden ya da baska bir moddan (YSM vb.)
 * kopyalanmamistir: assets/kuro/textures/yoii/chibi_default.png.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerTextureMixin {
   // Identifier.of(ns, path) icin path, "assets/<ns>/" sonrasindaki TAM yol olmali (textures/ onekiyle, .png uzantisiyla)
   private static final Identifier KURO$YOII_SKIN = Identifier.of("kuro", "textures/yoii/chibi_default.png");

   @Inject(method = "getTexture", at = @At("RETURN"), cancellable = true, require = 0)
   private void kuro$yoiiTexture(PlayerEntityRenderState state, CallbackInfoReturnable<Identifier> cir) {
      if (!Kuro.on[Kuro.YOII]) {
         return;
      }

      MinecraftClient mc = Kuro.mc;
      if (mc.player != null && state.id == mc.player.getId()) {
         return; // kendi derimizi degistirmiyoruz
      }

      cir.setReturnValue(kuro.Yoii.pick(state.id));
   }
}
