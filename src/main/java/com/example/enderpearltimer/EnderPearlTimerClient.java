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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderPearlTimerClient implements ClientModInitializer {

    public static final String MOD_ID = "enderpearltimer";
    private static final Logger LOGGER = LoggerFactory.getLogger("enderpearltimer");

    private static final int Y_OFFSET_FROM_BOTTOM = 49;

    private static final KeyBinding.Category KEY_CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    private static KeyBinding toggleTimerKey;
    private static boolean timerVisible = true;
    private static boolean firstRenderLogged = false;

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
            if (entity instanceof EnderPearlEntity pearl) {
                boolean isOwner = pearl.getOwner() == client.player;
                client.player.sendMessage(Text.literal(
                        "[PearlTimer-Debug] Perle erkannt. Owner=" + pearl.getOwner()
                                + " | ist eigener Spieler=" + isOwner
                ), false);

                if (isOwner) {
                    PearlTracker.startTracking(pearl);
                    client.player.sendMessage(Text.literal("[PearlTimer-Debug] Tracking gestartet."), false);
                }
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

        LOGGER.info("PearlTimer: registriere HUD-Element jetzt...");
        try {
            HudElementRegistry.addLast(
                    Identifier.of(MOD_ID, "pearl_timer"),
                    EnderPearlTimerClient::renderTimer
            );
            LOGGER.info("PearlTimer: HUD-Element erfolgreich registriert.");
        } catch (Throwable t) {
            LOGGER.error("PearlTimer: FEHLER beim Registrieren des HUD-Elements!", t);
        }
    }

    private static void renderTimer(DrawContext context, RenderTickCounter tickCounter) {
        try {
            if (!firstRenderLogged) {
                LOGGER.info("PearlTimer: renderTimer wurde zum ersten Mal aufgerufen.");
                firstRenderLogged = true;
            }

            MinecraftClient client = MinecraftClient.getInstance();

            if (client.options.hudHidden) {
                return;
            }

            context.drawTextWithShadow(client.textRenderer, Text.literal("PearlTimer HUD aktiv"), 4, 4, 0xFFFFFF00);

            if (!timerVisible) {
                return;
            }

            Text text;
            if (PearlTracker.isShowingBubbleColumnFlash()) {
                text = Text.literal("Blasensäule");
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
        } catch (Throwable t) {
            LOGGER.error("PearlTimer: FEHLER beim Rendern!", t);
        }
    }
}
