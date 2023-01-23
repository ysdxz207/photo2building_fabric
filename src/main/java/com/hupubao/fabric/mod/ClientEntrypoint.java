package com.hupubao.fabric.mod;

import com.hupubao.fabric.mod.MainScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ClientEntrypoint implements ClientModInitializer {
 
    // The KeyBinding declaration and registration are commonly executed here statically
    private static KeyBinding keyBinding;


    public void registerKeybinding() {

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.photo2building.main", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_G, // The keycode of the key
                "main.title" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                client.setScreen(new MainScreen(null));
//                client.player.sendMessage(new LiteralText("Key 1 was pressed!"), false);
            }
        });
    }

    @Override
    public void onInitializeClient() {
        registerKeybinding();
    }
}