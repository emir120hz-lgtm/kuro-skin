package kuro.mixin;

import kuro.Yoii;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Yoii: oyuncu modeli cizildikten sonra ek (kendi yazdigimiz) saç parçalarını çizer. */
@Mixin(Model.class)
public abstract class ModelRenderMixin {
   @Inject(method = "render", at = @At("TAIL"), require = 0)
   private void kuro$yoiiExtras(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color, CallbackInfo ci) {
      if (Yoii.extra && (Object)this instanceof PlayerEntityModel) {
         Yoii.renderExtras((PlayerEntityModel)(Object)this, matrices, vertices, light, overlay, color);
      }
   }
}
