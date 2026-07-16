package com.example.enderpearltimer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class EnderPearlTimerClient implements ClientModInitializer {

    public static final String MOD_ID = "enderpearltimer";

    private static final int Y_OFFSET_FROM_BOTTOM = 49;

    private static final KeyBinding.Category KEY_CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    private static KeyBinding toggleTimerKey;
    private static boolean timerVisible = true;

    @Override
    public void onInitializeClient() {

        toggleTimerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.enderpearltimer.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
                return;
            }
            if (entity instanceof EnderPearlEntity pearl && pearl.getOwner() == client.player) {
                PearlTracker.startTracking(pearl);
            }
        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (PearlTracker.isTracking(entity)) {
                PearlTracker.stopTracking();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PearlTracker.tick(client);
            while (toggleTimerKey.wasPressed()) {
                timerVisible = !timerVisible;
            }
        });

        HudElementRegistry.addLast(
                Identifier.of(MOD_ID, "pearl_timer"),
                EnderPearlTimerClient::renderTimer
        );
    }

    private static void renderTimer(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.options.hudHidden || !timerVisible) {
            return;
        }

        Text text;
        if (PearlTracker.isShowingBubbleColumnFlash()) {
            text = Text.literal("BubbelColuem");
        } else {
            Float secondsRemaining = PearlTracker.getSecondsRemaining();
            if (secondsRemaining == null) {
                return;
            }
            float clamped = Math.max(0.0f, secondsRemaining);
            text = Text.literal(String.format("%.1fs", clamped));
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        int textWidth = client.textRenderer.getWidth(text);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - Y_OFFSET_FROM_BOTTOM;

        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFFFF);
    }
}
