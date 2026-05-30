package com.crpg.ebb.client.input;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.client.network.ClientInteractionNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ClientKeyMappings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(EbbMod.id("controls"));
    public static final KeyMapping INTERACT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.ebb.interact",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    ));

    private ClientKeyMappings() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (INTERACT.consumeClick()) {
                ClientInteractionNetworking.sendCurrentTargetInteraction(client);
            }
        });
    }
}
