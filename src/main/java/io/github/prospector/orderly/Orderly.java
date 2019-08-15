package io.github.prospector.orderly;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.prospector.orderly.config.OrderlyConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class Orderly implements ClientModInitializer {

    public static final String MOD_ID = "orderly";
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();
    private static FabricKeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        OrderlyConfigManager.initializeConfig();
        toggleKey = FabricKeyBinding.Builder.create(new Identifier(Orderly.MOD_ID, "toggle"), InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEYCODE.getKeyCode(), "key.categories.misc").build();
        KeyBindingRegistry.INSTANCE.register(toggleKey);
        ClientTickCallback.EVENT.register(event -> {
            if (event.isWindowFocused() && toggleKey.wasPressed()) {
                OrderlyConfigManager.getConfig().toggleDraw();
                OrderlyConfigManager.save();
            }
        });
    }
}
