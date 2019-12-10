package io.github.prospector.orderly;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.prospector.orderly.config.OrderlyConfig;
import io.github.prospector.orderly.config.OrderlyConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.RayTraceContext;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.StreamSupport;

public class HealthBarRenderer {

    public static void render(MatrixStack matrices, float partialTicks, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projection, Frustum capturedFrustum) {
        MinecraftClient mc = MinecraftClient.getInstance();
        OrderlyConfig config = OrderlyConfigManager.getConfig();
        if(mc.world == null || (!config.canRenderInF1() && !MinecraftClient.isHudEnabled()) || !config.canDraw()) {
            return;
        }
        final Entity cameraEntity = camera.getFocusedEntity() != null ? camera.getFocusedEntity() : mc.player; //possible fix for optifine (see https://github.com/UpcraftLP/Orderly/issues/3)
        assert cameraEntity != null : "Camera Entity must not be null!";
        if(config.showingOnlyFocused()) {
            Entity focused = getEntityLookedAt(cameraEntity);
            if(focused instanceof LivingEntity && focused.isAlive()) {
                renderHealthBar((LivingEntity) focused, matrices, partialTicks, camera, cameraEntity);
            }
        }
        else {
            Vec3d cameraPos = camera.getPos();
            final Frustum frustum;
            if(capturedFrustum != null) {
                frustum = capturedFrustum;
            }
            else {
                frustum = new Frustum(matrices.peek().getModel(), projection);
                frustum.setPosition(cameraPos.getX(), cameraPos.getY(), cameraPos.getZ());
            }
            StreamSupport.stream(mc.world.getEntities().spliterator(), false).filter(entity -> entity instanceof LivingEntity && entity != cameraEntity && entity.isAlive() && entity.getPassengersDeep().isEmpty() && entity.shouldRender(cameraPos.getX(), cameraPos.getY(), cameraPos.getZ()) && (entity.ignoreCameraFrustum || frustum.isVisible(entity.getBoundingBox()))).map(LivingEntity.class::cast).forEach(entity -> renderHealthBar(entity, matrices, partialTicks, camera, cameraEntity));
        }
    }

    private static Entity getEntityLookedAt(Entity e) {
        Entity foundEntity = null;
        final double finalDistance = 32;
        double distance = finalDistance;
        HitResult pos = raycast(e, finalDistance);
        Vec3d positionVector = e.getPosVector();
        if(e instanceof PlayerEntity) {
            positionVector = positionVector.add(0, e.getEyeHeight(e.getPose()), 0);
        }
        if(pos != null) {
            distance = pos.getPos().distanceTo(positionVector);
        }
        Vec3d lookVector = e.getRotationVector();
        Vec3d reachVector = positionVector.add(lookVector.x * finalDistance, lookVector.y * finalDistance, lookVector.z * finalDistance);
        Entity lookedEntity = null;
        List<Entity> entitiesInBoundingBox = e.getEntityWorld().getEntities(e, e.getBoundingBox().stretch(lookVector.x * finalDistance, lookVector.y * finalDistance, lookVector.z * finalDistance).expand(1.0F));
        double minDistance = distance;
        for(Entity entity : entitiesInBoundingBox) {
            if(entity.collides()) {
                Box collisionBox = entity.getVisibilityBoundingBox();
                Optional<Vec3d> interceptPosition = collisionBox.rayTrace(positionVector, reachVector);
                if(collisionBox.contains(positionVector)) {
                    if(0.0D < minDistance || minDistance == 0.0D) {
                        lookedEntity = entity;
                        minDistance = 0.0D;
                    }
                }
                else if(interceptPosition.isPresent()) {
                    double distanceToEntity = positionVector.distanceTo(interceptPosition.get());
                    if(distanceToEntity < minDistance || minDistance == 0.0D) {
                        lookedEntity = entity;
                        minDistance = distanceToEntity;
                    }
                }
            }
            if(lookedEntity != null && (minDistance < distance || pos == null)) {
                foundEntity = lookedEntity;
            }
        }
        return foundEntity;
    }

    private static void renderHealthBar(LivingEntity passedEntity, MatrixStack matrices, float partialTicks, Camera camera, Entity viewPoint) {
        Preconditions.checkNotNull(passedEntity, "tried to render health bar for null entity");
        OrderlyConfig config = OrderlyConfigManager.getConfig();
        MinecraftClient mc = MinecraftClient.getInstance();
        Stack<LivingEntity> passengerStack = new Stack<>();
        LivingEntity entity = passedEntity;
        passengerStack.push(entity);
        while(entity.getPrimaryPassenger() instanceof LivingEntity) {
            entity = (LivingEntity) entity.getPrimaryPassenger();
            passengerStack.push(entity);
        }
        float pastTranslate = 0.0F;
        while(!passengerStack.isEmpty()) {
            entity = passengerStack.pop();
            if(!entity.isAlive()) continue;
            Identifier entityID = Registry.ENTITY_TYPE.getId(entity.getType());
            String idString = String.valueOf(entityID);
            boolean boss = config.getBosses().contains(idString);
            if(config.getBlacklist().contains(idString)) {
                continue;
            }
            processing:
            {
                float distance = passedEntity.distanceTo(viewPoint);
                if(distance > config.getMaxDistance() || !passedEntity.canSee(viewPoint) || entity.isInvisible()) {
                    break processing;
                }
                if(!config.canShowOnBosses() && boss) {
                    break processing;
                }
                if(!config.canShowOnPlayers() && entity instanceof PlayerEntity) {
                    break processing;
                }
                float maxHealth = entity.getMaximumHealth();
                if(maxHealth <= 0.0F) {
                    break processing;
                }
                double x = passedEntity.prevX + (passedEntity.getX() - passedEntity.prevX) * partialTicks;
                double y = passedEntity.prevY + (passedEntity.getY() - passedEntity.prevY) * partialTicks;
                double z = passedEntity.prevZ + (passedEntity.getZ() - passedEntity.prevZ) * partialTicks;
                float scale = 0.026666672F * config.getHealthBarScale();
                float health = MathHelper.clamp(entity.getHealth(), 0.0F, maxHealth);
                float percent = (health / maxHealth) * 100.0F;
                EntityRenderDispatcher renderManager = MinecraftClient.getInstance().getEntityRenderManager();
                int bgHeight = config.getBackgroundHeight();
                int barHeight = config.getBarHeight();
                float padding = config.getBackgroundPadding();
                matrices.push();
                {
                    matrices.translate(x - renderManager.camera.getPos().x, y - renderManager.camera.getPos().y + passedEntity.getHeight() + config.getHeightAbove(), z - renderManager.camera.getPos().z);
                    GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                    Quaternion rotation = camera.getRotation().copy();
                    rotation.scale(-1.0F);
                    matrices.multiply(rotation);
                    matrices.scale(-scale, -scale, scale);
                    boolean lighting = GL11.glGetBoolean(GL11.GL_LIGHTING);
                    RenderSystem.disableLighting();
                    RenderSystem.depthMask(false);
                    RenderSystem.disableDepthTest();
                    RenderSystem.disableTexture();
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
                    Tessellator tessellator = Tessellator.getInstance();
                    BufferBuilder buffer = tessellator.getBuffer();
                    float size = config.getPlateSize();
                    int r = 0;
                    int g = 255;
                    int b = 0;
                    ItemStack stack = null;
                    if(entity instanceof MobEntity) {
                        r = 255;
                        g = 0;
                        EntityGroup attr = entity.getGroup();
                        if(attr == EntityGroup.ARTHROPOD) {
                            stack = new ItemStack(Items.SPIDER_EYE);
                        }
                        else if(attr == EntityGroup.UNDEAD) {
                            stack = new ItemStack(Items.ROTTEN_FLESH);
                        }
                        else {
                            stack = new ItemStack(Items.SKELETON_SKULL, 1);
                        }
                    }
                    if(boss) {
                        stack = new ItemStack(Items.WITHER_SKELETON_SKULL);
                        size = config.getPlateSizeBoss();
                        r = 128;
                        g = 0;
                        b = 128;
                    }
                    int armor = entity.getArmor();
                    boolean useHue = !config.colorByType();
                    if(useHue) {
                        float hue = Math.max(0.0F, (health / maxHealth) / 3.0F - 0.07F);
                        Color color = Color.getHSBColor(hue, 1.0F, 1.0F);
                        r = color.getRed();
                        g = color.getGreen();
                        b = color.getBlue();
                    }
                    matrices.translate(0.0F, pastTranslate, 0.0F);
                    float s = 0.5F;
                    String name = (entity.hasCustomName() ? entity.getCustomName().formatted(Formatting.ITALIC) : entity.getDisplayName()).asFormattedString();
                    float namel = mc.textRenderer.getStringWidth(name) * s;
                    if(namel + 20 > size * 2) {
                        size = namel / 2.0F + 10.0F;
                    }
                    float healthSize = size * (health / maxHealth);
                    // Background
                    if(config.drawsBackground()) {
                        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
                        buffer.vertex(-size - padding, -bgHeight, 0.0D).color(0, 0, 0, 64).next();
                        buffer.vertex(-size - padding, barHeight + padding, 0.0D).color(0, 0, 0, 64).next();
                        buffer.vertex(size + padding, barHeight + padding, 0.0D).color(0, 0, 0, 64).next();
                        buffer.vertex(size + padding, -bgHeight, 0.0D).color(0, 0, 0, 64).next();
                        tessellator.draw();
                    }
                    // Gray Space
                    buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
                    buffer.vertex(-size, 0, 0.0D).color(127, 127, 127, 127).next();
                    buffer.vertex(-size, barHeight, 0.0D).color(127, 127, 127, 127).next();
                    buffer.vertex(size, barHeight, 0.0D).color(127, 127, 127, 127).next();
                    buffer.vertex(size, 0, 0.0D).color(127, 127, 127, 127).next();
                    tessellator.draw();
                    // Health Bar
                    buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
                    buffer.vertex(-size, 0, 0.0D).color(r, g, b, 127).next();
                    buffer.vertex(-size, barHeight, 0.0D).color(r, g, b, 127).next();
                    buffer.vertex(healthSize * 2 - size, barHeight, 0.0D).color(r, g, b, 127).next();
                    buffer.vertex(healthSize * 2 - size, 0, 0.0D).color(r, g, b, 127).next();
                    tessellator.draw();
                    RenderSystem.enableTexture();
                    matrices.push();
                    {
                        matrices.translate(-size, -4.5F, 0.0F);
                        matrices.scale(s, s, s);
                        mc.textRenderer.draw(name, 0, 0, 0xFFFFFF);
                        float s1 = 0.75F;
                        matrices.push();
                        {
                            matrices.scale(s1, s1, s1);
                            int h = config.getHpTextHeight();
                            String maxHpStr = String.format("%s%.2f", Formatting.BOLD, maxHealth);
                            String hpStr = String.format("%.2f", health);
                            String percStr = String.format("%.2f%%", percent);
                            if(maxHpStr.endsWith(".0")) {
                                maxHpStr = maxHpStr.substring(0, maxHpStr.length() - 2);
                            }
                            if(hpStr.endsWith(".0")) {
                                hpStr = hpStr.substring(0, hpStr.length() - 2);
                            }
                            if(config.canCurrentHP()) {
                                mc.textRenderer.draw(hpStr, 2, h, 0xFFFFFF);
                            }
                            if(config.canShowMaxHP()) {
                                mc.textRenderer.draw(maxHpStr, (int) (size / (s * s1) * 2) - 2 - mc.textRenderer.getStringWidth(maxHpStr), h, 0xFFFFFF);
                            }
                            if(config.canShowPercentage()) {
                                mc.textRenderer.draw(percStr, (int) (size / (s * s1)) - mc.textRenderer.getStringWidth(percStr) / 2.0F, h, 0xFFFFFFFF);
                            }
                            if(config.isDebugInfoEnabled() && mc.options.debugEnabled) {
                                mc.textRenderer.draw(String.format("ID: \"%s\"", idString), 0, h + 16, 0xFFFFFFFF);
                            }
                        }
                        matrices.pop();
                        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                        int off = 0;
                        s1 = 0.5F;
                        matrices.scale(s1, s1, s1);
                        matrices.translate(size / (s * s1) * 2 - 16, 0.0F, 0.0F);
                        mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
                        if(stack != null && config.canShowAttributes()) {
                            renderIcon(off, 0, stack, 16, 16);
                            off -= 16;
                        }
                        if(armor > 0 && config.canShowArmor()) {
                            int ironArmor = armor % 5;
                            int diamondArmor = armor / 5;
                            if(!config.canShowGroupArmor()) {
                                ironArmor = armor;
                                diamondArmor = 0;
                            }
                            stack = new ItemStack(Items.IRON_CHESTPLATE);
                            for(int i = 0; i < ironArmor; i++) {
                                renderIcon(off, 0, stack, 16, 16);
                                off -= 4;
                            }
                            stack = new ItemStack(Items.DIAMOND_CHESTPLATE);
                            for(int i = 0; i < diamondArmor; i++) {
                                renderIcon(off, 0, stack, 16, 16);
                                off -= 4;
                            }
                        }
                    }
                    matrices.pop();
                    RenderSystem.disableBlend();
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthMask(true);
                    if(lighting) {
                        RenderSystem.enableLighting();
                    }
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                }
                matrices.pop();
                pastTranslate -= bgHeight + barHeight + padding;
            }
        }
    }

    @Nullable
    private static HitResult raycast(Entity entity, double len) {
        Vec3d vec = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        if(entity instanceof PlayerEntity) {
            vec = vec.add(new Vec3d(0, entity.getEyeHeight(entity.getPose()), 0));
        }
        Vec3d look = entity.getRotationVector();
        if(look == null) {
            return null;
        }
        return raycast(entity, vec, look, len);
    }

    private static void renderIcon(int vertexX, int vertexY, ItemStack stack, int intU, int intV) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            BakedModel bakedModel = mc.getItemRenderer().getModels().getModel(stack);
            Sprite textureAtlasSprite = bakedModel.getSprite();
            mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
            buffer.vertex(vertexX, vertexY + intV, 0.0D).texture(textureAtlasSprite.getMinU(), textureAtlasSprite.getMaxV()).next();
            buffer.vertex(vertexX + intU, vertexY + intV, 0.0D).texture(textureAtlasSprite.getMaxU(), textureAtlasSprite.getMaxV()).next();
            buffer.vertex(vertexX + intU, vertexY, 0.0D).texture(textureAtlasSprite.getMaxU(), textureAtlasSprite.getMinV()).next();
            buffer.vertex(vertexX, vertexY, 0.0D).texture(textureAtlasSprite.getMinU(), textureAtlasSprite.getMinV()).next();
            tessellator.draw();
        }
        catch (Exception ignore) {
            //TODO exception handling?
        }
    }

    private static HitResult raycast(Entity entity, Vec3d origin, Vec3d ray, double len) {
        Vec3d next = origin.add(ray.normalize().multiply(len));
        return entity.getEntityWorld().rayTrace(new RayTraceContext(origin, next, RayTraceContext.ShapeType.OUTLINE, RayTraceContext.FluidHandling.NONE, entity));
    }
}
