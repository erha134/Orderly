package io.github.prospector.orderly.config;

import io.github.prospector.modmenu.api.ModMenuApi;
import io.github.prospector.orderly.Orderly;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

@SuppressWarnings("unused")
public class OrderlyModMenuCompat implements ModMenuApi {
    @Override
    public String getModId() {
        return Orderly.MODID;
    }

    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory() {
        return OrderlyConfigImpl::createConfigScreen;
    }
}
