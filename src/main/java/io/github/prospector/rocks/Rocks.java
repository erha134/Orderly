package io.github.prospector.rocks;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.prospector.rocks.config.RocksConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class Rocks implements ClientModInitializer {

    public static final String MOD_ID = "rocks";
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();
    public FabricKeyBinding toggleKey;
    public boolean togglePressed;

    @Override
    public void onInitializeClient() {
        RocksConfigManager.initializeConfig();
        toggleKey = FabricKeyBinding.Builder.create(new Identifier(Rocks.MOD_ID, "toggle"), InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEYCODE.getKeyCode(), "key.categories.misc").build();
        KeyBindingRegistry.INSTANCE.register(toggleKey);
        ClientTickCallback.EVENT.register(event -> {
            boolean wasDown = togglePressed;
            togglePressed = toggleKey.isPressed();
            if (event.isWindowFocused() && togglePressed && !wasDown) {
                RocksConfigManager.getConfig().toggleDraw();
            }
        });
    }
}
