package io.github.prospector.orderly.api.config;

import java.util.Set;

public interface OrderlyConfig {
    boolean canDraw();

    int getMaxDistance();

    boolean canRenderInF1();

    float getHealthBarScale();

    double getHeightAbove();

    boolean drawsBackground();

    int getBackgroundPadding();

    int getBackgroundHeight();

    int getBarHeight();

    int getPlateSize();

    int getPlateSizeBoss();

    boolean canShowAttributes();

    boolean canShowArmor();

    boolean canShowGroupArmor();

    boolean colorByType();

    int getHpTextHeight();

    boolean canShowMaxHP();

    boolean showCurrentHP();

    boolean canShowPercentage();

    boolean canShowOnPlayers();

    boolean canShowOnBosses();

    boolean showingOnlyFocused();

    boolean isDebugInfoEnabled();

    Set<String> getBlacklist();

    Set<String> getBosses();
}
