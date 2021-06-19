package io.github.prospector.orderly.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.prospector.orderly.Orderly;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

@SuppressWarnings("unused")
public class OrderlyModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OrderlyConfigImpl::createConfigScreen;
    }
}
