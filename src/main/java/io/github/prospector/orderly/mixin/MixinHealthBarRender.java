package io.github.prospector.orderly.mixin;

import io.github.prospector.orderly.HealthBarRenderer;
import io.github.prospector.orderly.ui.DefaultUIStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(WorldRenderer.class)
public class MixinHealthBarRender {


    @Shadow
    @Nullable
    private Frustum capturedFrustum;

//    @Shadow
//    public ShaderEffect transparencyShader;


    @Inject(method = "render",
            at = @At(value = "RETURN"))
    private void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projection, CallbackInfo ci) {
        HealthBarRenderer.render(matrices, tickDelta, camera, gameRenderer, lightmapTextureManager, projection, this.capturedFrustum);
//        if (this.transparencyShader != null) {
//            //this.transparencyShader.render(tickDelta);
//        }
    }
}
