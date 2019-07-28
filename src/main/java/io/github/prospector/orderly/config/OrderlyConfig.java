package io.github.prospector.orderly.config;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class OrderlyConfig {

    private boolean draw = true;

    private int maxDistance = 24;
    private boolean renderInF1 = false;
    private double heightAbove = 0.6;
    private boolean drawBackground = true;
    private int backgroundPadding = 2;
    private int backgroundHeight = 6;
    private int barHeight = 4;
    private int plateSize = 25;
    private int plateSizeBoss = 50;
    private boolean showAttributes = true;
    private boolean showArmor = true;
    private boolean groupArmor = true;
    private boolean colorByType = false;
    private int hpTextHeight = 14;
    private boolean showMaxHP = true;
    private boolean showCurrentHP = true;
    private boolean showPercentage = true;
    private boolean showOnPlayers = true;
    private boolean showOnBosses = true;
    private boolean showOnlyFocused = false;
    private boolean enableDebugInfo = true;
    private final Set<String> blacklist = ImmutableSet.of("minecraft:shulker", "minecraft:armor_stand", "minecraft:cod", "minecraft:salmon", "minecraft:pufferfish", "minecraft:tropical_fish");


    public void toggleDraw() {
        draw = !draw;
        OrderlyConfigManager.save();
    }

    public boolean canDraw() {
        return draw;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public boolean canRenderInF1() {
        return renderInF1;
    }

    public double getHeightAbove() {
        return heightAbove;
    }

    public boolean drawsBackground() {
        return drawBackground;
    }

    public int getBackgroundPadding() {
        return backgroundPadding;
    }

    public int getBackgroundHeight() {
        return backgroundHeight;
    }

    public int getBarHeight() {
        return barHeight;
    }

    public int getPlateSize() {
        return plateSize;
    }

    public int getPlateSizeBoss() {
        return plateSizeBoss;
    }

    public boolean canShowAttributes() {
        return showAttributes;
    }

    public boolean canShowArmor() {
        return showArmor;
    }

    public boolean canShowGroupArmor() {
        return groupArmor;
    }

    public boolean colorByType() {
        return colorByType;
    }

    public int getHpTextHeight() {
        return hpTextHeight;
    }

    public boolean canShowMaxHP() {
        return showMaxHP;
    }

    public boolean canCurrentHP() {
        return showCurrentHP;
    }

    public boolean canShowPercentage() {
        return showPercentage;
    }

    public boolean canShowOnPlayers() {
        return showOnPlayers;
    }

    public boolean canShowOnBosses() {
        return showOnBosses;
    }

    public boolean showingOnlyFocused() {
        return showOnlyFocused;
    }

    public boolean isDebugInfoEnabled() {
        return enableDebugInfo;
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }

//            v_maxDistance = builder.define("Max Distance", maxDistance);
//            v_renderInF1 = builder.define("Render with Interface Disabled (F1)", renderInF1);
//            v_heightAbove = builder.define("Height Above Mob", heightAbove);
//            v_drawBackground = builder.define("Draw Background", drawBackground);
//            v_backgroundPadding = builder.define("Background Padding", backgroundPadding);
//            v_backgroundHeight = builder.define("Background Height", backgroundHeight);
//            v_barHeight = builder.define("Health Bar Height", barHeight);
//            v_plateSize = builder.define("Plate Size", plateSize);
//            v_plateSizeBoss = builder.define("Plate Size (Boss)", plateSizeBoss);
//            v_showAttributes = builder.define("Show Attributes", showAttributes);
//            v_showArmor = builder.define("Show Armor", showArmor);
//            v_groupArmor = builder.define("Group Armor (condense 5 iron icons into 1 diamond icon)", groupArmor);
//            v_colorByType = builder.define("Color Health Bar by Type (instead of health percentage)", colorByType);
//            v_hpTextHeight = builder.define("HP Text Height", hpTextHeight);
//            v_showMaxHP = builder.define("Show Max HP", showMaxHP);
//            v_showCurrentHP = builder.define("Show Current HP", showCurrentHP);
//            v_showPercentage = builder.define("Show HP Percentage", showPercentage);
//            v_showOnPlayers = builder.define("Display on Players", showOnPlayers);
//            v_showOnBosses = builder.define("Display on Bosses", showOnBosses);
//            v_showOnlyFocused = builder.define("Only show the health bar for the entity looked at", showOnlyFocused);
//            v_enableDebugInfo = builder.define("Show Debug Info with F3", enableDebugInfo);
//            v_blacklist = builder.comment("Blacklist uses entity IDs, not their display names. Use F3 to see them in the Orderly bar.")


}
