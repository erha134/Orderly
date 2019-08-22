package io.github.prospector.orderly;

import io.github.prospector.orderly.config.OrderlyConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Orderly implements ClientModInitializer {

    public static final String MOD_ID = "orderly";
    private static FabricKeyBinding toggleKey;
    private static final Logger log = LogManager.getFormatterLogger(MOD_ID);

    public static Logger getLogger() {
        return log;
    }

    @Override
    public void onInitializeClient() {
        OrderlyConfigManager.init();
        toggleKey = FabricKeyBinding.Builder.create(new Identifier(Orderly.MOD_ID, "toggle"), InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEYCODE.getKeyCode(), "key.categories.misc").build();
        KeyBindingRegistry.INSTANCE.register(toggleKey);
        ClientTickCallback.EVENT.register(event -> {
            if (event.isWindowFocused() && toggleKey.wasPressed()) {
                OrderlyConfigManager.getConfig().toggleDraw();
                OrderlyConfigManager.save(true);
            }
        });
    }
}
