package vazkii.neat.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.neat.HealthBarRenderer;

@Mixin(WorldRenderer.class)
public class MixinHealthBarRender {
    @Inject(method = "renderEntities", at = @At("TAIL"))
    public void render(Camera camera, VisibleRegion visibleRegion, float delta, CallbackInfo info) {
        HealthBarRenderer.render(delta);
    }
}
