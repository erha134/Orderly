package vazkii.neat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class Neat implements ClientModInitializer {

    public static final String MOD_ID = "neat";
    public FabricKeyBinding toggleKey;
    public boolean togglePressed;


    @Override
    public void onInitializeClient() {
        NeatConfig.init();
        NeatConfig.load();
        toggleKey = FabricKeyBinding.Builder.create(new Identifier(Neat.MOD_ID, "toggle"), InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEYCODE.getKeyCode(), "key.categories.misc").build();
        KeyBindingRegistry.INSTANCE.register(toggleKey);
        ClientTickCallback.EVENT.register(event -> {
            boolean wasDown = togglePressed;
            togglePressed = toggleKey.isPressed();
            if (event.isWindowFocused() && togglePressed && !wasDown) {
                NeatConfig.draw = !NeatConfig.draw;
            }
        });
    }
}
