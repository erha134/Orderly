package io.github.prospector.orderly;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.prospector.orderly.config.OrderlyConfig;
import io.github.prospector.orderly.config.OrderlyConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.FrustumWithOrigin;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
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

    public static void render(float partialTicks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        OrderlyConfig config = OrderlyConfigManager.getConfig();
        if((!config.canRenderInF1() && !MinecraftClient.isHudEnabled()) || !config.canDraw()) {
            return;
        }
        final Entity cameraEntity = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player; //possible fix for optifine (see https://github.com/UpcraftLP/Orderly/issues/3)
        BlockPos renderingVector = cameraEntity.getBlockPos();
        FrustumWithOrigin frustum = new FrustumWithOrigin();
        double viewX = cameraEntity.prevX + (cameraEntity.x - cameraEntity.prevX) * partialTicks;
        double viewY = cameraEntity.prevY + (cameraEntity.y - cameraEntity.prevY) * partialTicks;
        double viewZ = cameraEntity.prevZ + (cameraEntity.z - cameraEntity.prevZ) * partialTicks;
        frustum.setOrigin(viewX, viewY, viewZ);
        if(config.showingOnlyFocused()) {
            Entity focused = getEntityLookedAt(cameraEntity);
            if(focused instanceof LivingEntity && focused.isAlive()) {
                renderHealthBar((LivingEntity) focused, partialTicks, cameraEntity);
            }
        }
        else {
            StreamSupport.stream(mc.world.getEntities().spliterator(), false).filter(entity -> entity instanceof LivingEntity && entity != cameraEntity && entity.isAlive() && entity.getPassengersDeep().isEmpty() && entity.shouldRenderFrom(renderingVector.getX(), renderingVector.getY(), renderingVector.getZ()) && (entity.ignoreCameraFrustum || frustum.intersects(entity.getBoundingBox()))).map(LivingEntity.class::cast).forEach(entity -> renderHealthBar(entity, partialTicks, cameraEntity));
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

    private static void renderHealthBar(LivingEntity passedEntity, float partialTicks, Entity viewPoint) {
        OrderlyConfig config = OrderlyConfigManager.getConfig();
        Stack<LivingEntity> passengerStack = new Stack<>();
        LivingEntity entity = passedEntity;
        passengerStack.push(entity);
        while(entity.getPrimaryPassenger() instanceof LivingEntity) {
            entity = (LivingEntity) entity.getPrimaryPassenger();
            passengerStack.push(entity);
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        float pastTranslate = 0.0F;
        while(!passengerStack.isEmpty()) {
            entity = passengerStack.pop();
            boolean boss = !entity.canUsePortals();
            Identifier entityID = Registry.ENTITY_TYPE.getId(entity.getType());
            if(config.getBlacklist().contains(entityID.toString())) {
                continue;
            }
            processing:
            {
                float distance = passedEntity.distanceTo(viewPoint);
                if(distance > config.getMaxDistance() || !passedEntity.canSee(viewPoint) || entity.isInvisible()) {
                    break processing;
                }
                if(!config.canShowOnBosses() && !boss) {
                    break processing;
                }
                if(!config.canShowOnPlayers() && entity instanceof PlayerEntity) {
                    break processing;
                }
                float maxHealth = entity.getHealthMaximum();
                if(maxHealth <= 0.0F) {
                    break processing;
                }
                double x = passedEntity.prevX + (passedEntity.x - passedEntity.prevX) * partialTicks;
                double y = passedEntity.prevY + (passedEntity.y - passedEntity.prevY) * partialTicks;
                double z = passedEntity.prevZ + (passedEntity.z - passedEntity.prevZ) * partialTicks;
                float scale = 0.026666672F;
                float health = MathHelper.clamp(entity.getHealth(), 0.0F, maxHealth);
                float percent = (health / maxHealth) * 100.0F;
                EntityRenderDispatcher renderManager = MinecraftClient.getInstance().getEntityRenderManager();
                GlStateManager.pushMatrix();
                GlStateManager.translatef((float) (x - renderManager.camera.getPos().x), (float) (y - renderManager.camera.getPos().y + passedEntity.getHeight() + config.getHeightAbove()), (float) (z - renderManager.camera.getPos().z));
                GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                GlStateManager.rotatef(-renderManager.cameraYaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotatef(renderManager.cameraPitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.scalef(-scale, -scale, scale);
                boolean lighting = GL11.glGetBoolean(GL11.GL_LIGHTING);
                GlStateManager.disableLighting();
                GlStateManager.depthMask(false);
                GlStateManager.disableDepthTest();
                GlStateManager.disableTexture();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBufferBuilder();
                float padding = config.getBackgroundPadding();
                int bgHeight = config.getBackgroundHeight();
                int barHeight = config.getBarHeight();
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
                GlStateManager.translatef(0.0F, pastTranslate, 0.0F);
                float s = 0.5F;
                String name = (entity.hasCustomName() ? entity.getCustomName().formatted(Formatting.ITALIC) : entity.getDisplayName()).asFormattedString();
                float namel = mc.textRenderer.getStringWidth(name) * s;
                if(namel + 20 > size * 2) {
                    size = namel / 2.0F + 10.0F;
                }
                float healthSize = size * (health / maxHealth);
                // Background
                if(config.drawsBackground()) {
                    buffer.begin(7, VertexFormats.POSITION_COLOR);
                    buffer.vertex(-size - padding, -bgHeight, 0.0D).color(0, 0, 0, 64).next();
                    buffer.vertex(-size - padding, barHeight + padding, 0.0D).color(0, 0, 0, 64).next();
                    buffer.vertex(size + padding, barHeight + padding, 0.0D).color(0, 0, 0, 64).next();
                    buffer.vertex(size + padding, -bgHeight, 0.0D).color(0, 0, 0, 64).next();
                    tessellator.draw();
                }
                // Gray Space
                buffer.begin(7, VertexFormats.POSITION_COLOR);
                buffer.vertex(-size, 0, 0.0D).color(127, 127, 127, 127).next();
                buffer.vertex(-size, barHeight, 0.0D).color(127, 127, 127, 127).next();
                buffer.vertex(size, barHeight, 0.0D).color(127, 127, 127, 127).next();
                buffer.vertex(size, 0, 0.0D).color(127, 127, 127, 127).next();
                tessellator.draw();
                // Health Bar
                buffer.begin(7, VertexFormats.POSITION_COLOR);
                buffer.vertex(-size, 0, 0.0D).color(r, g, b, 127).next();
                buffer.vertex(-size, barHeight, 0.0D).color(r, g, b, 127).next();
                buffer.vertex(healthSize * 2 - size, barHeight, 0.0D).color(r, g, b, 127).next();
                buffer.vertex(healthSize * 2 - size, 0, 0.0D).color(r, g, b, 127).next();
                tessellator.draw();
                GlStateManager.enableTexture();
                GlStateManager.pushMatrix();
                GlStateManager.translatef(-size, -4.5F, 0.0F);
                GlStateManager.scalef(s, s, s);
                mc.textRenderer.draw(name, 0, 0, 0xFFFFFF);
                GlStateManager.pushMatrix();
                float s1 = 0.75F;
                GlStateManager.scalef(s1, s1, s1);
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
                    mc.textRenderer.draw("ID: \"" + entityID + "\"", 0, h + 16, 0xFFFFFFFF);
                }
                GlStateManager.popMatrix();
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int off = 0;
                s1 = 0.5F;
                GlStateManager.scalef(s1, s1, s1);
                GlStateManager.translatef(size / (s * s1) * 2 - 16, 0.0F, 0.0F);
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
                GlStateManager.popMatrix();
                GlStateManager.disableBlend();
                GlStateManager.enableDepthTest();
                GlStateManager.depthMask(true);
                if(lighting) {
                    GlStateManager.enableLighting();
                }
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
                pastTranslate -= bgHeight + barHeight + padding;
            }
        }
    }

    @Nullable
    private static HitResult raycast(Entity entity, double len) {
        Vec3d vec = new Vec3d(entity.x, entity.y, entity.z);
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
            BakedModel bakedModel = mc.getItemRenderer().getModel(stack);
            Sprite textureAtlasSprite = mc.getSpriteAtlas().getSprite(bakedModel.getSprite().getId());
            mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBufferBuilder();
            buffer.begin(7, VertexFormats.POSITION_UV);
            buffer.vertex(vertexX, vertexY + intV, 0.0D).texture(textureAtlasSprite.getMinU(), textureAtlasSprite.getMaxV()).next();
            buffer.vertex(vertexX + intU, vertexY + intV, 0.0D).texture(textureAtlasSprite.getMaxU(), textureAtlasSprite.getMaxV()).next();
            buffer.vertex(vertexX + intU, vertexY, 0.0D).texture(textureAtlasSprite.getMaxU(), textureAtlasSprite.getMinV()).next();
            buffer.vertex(vertexX, vertexY, 0.0D).texture(textureAtlasSprite.getMinU(), textureAtlasSprite.getMinV()).next();
            tessellator.draw();
        }
        catch (Exception e) {
        }
    }

    private static HitResult raycast(Entity entity, Vec3d origin, Vec3d ray, double len) {
        Vec3d next = origin.add(ray.normalize().multiply(len));
        return entity.getEntityWorld().rayTrace(new RayTraceContext(origin, next, RayTraceContext.ShapeType.OUTLINE, RayTraceContext.FluidHandling.NONE, entity));
    }
}
