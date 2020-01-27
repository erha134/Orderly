package io.github.prospector.orderly.ui;

import io.github.prospector.orderly.api.UIStyle;
import io.github.prospector.orderly.api.config.OrderlyConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public abstract class SimpleUIStyle implements UIStyle {

    protected abstract void render(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Camera camera, OrderlyConfig config, LivingEntity entity, int light, @Nullable ItemStack icon, boolean boss);

    @Override
    public void renderEntity(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Camera camera, OrderlyConfig config, LivingEntity entity, int light, ItemStack icon) {
        this.render(matrices, immediate, camera, config, entity, light, icon, false);
    }

    @Override
    public void renderBossEntity(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Camera camera, OrderlyConfig config, LivingEntity entity, int light, ItemStack icon) {
        this.render(matrices, immediate, camera, config, entity, light, icon, true);
    }
}
