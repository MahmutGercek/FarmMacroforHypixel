package com.example.keyloop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

@Mod(modid = "keyloopmod", name = "Key Loop Mod", version = "1.0", clientSideOnly = true)
public class keyloop {
    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;
    private boolean wasShiftDown = false;

    private long lastMoveCheckTime = 0;
    private double lastX = 0;
    private double lastZ = 0;

    private State currentState = State.HOLD_A;

    private enum State {
        HOLD_A, HOLD_W, HOLD_D,HOLD_W2
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);

    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!enabled) return; // Eğer mod devrede değilse işlem yapma

        if (event.gui instanceof net.minecraft.client.gui.GuiIngameMenu) {
            event.setCanceled(true); // ESC menüsünü engelle
        }
    }


    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {

        if (mc.thePlayer == null) return;

        // Shift toggle
        boolean isShiftDown = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (isShiftDown && !wasShiftDown) {
            enabled = !enabled;
            if (!enabled) {
                releaseAllKeys();
            } else {
                currentState = State.HOLD_A;
                holdCurrentKey();
                lastX = mc.thePlayer.posX;
                lastZ = mc.thePlayer.posZ;
                lastMoveCheckTime = System.currentTimeMillis();
            }
        }
        wasShiftDown = isShiftDown;

        if (!enabled) return;

        // Mouse left click
        if (!Mouse.isButtonDown(0)) {
            Mouse.poll();
            Mouse.next();
            if (mc.thePlayer != null && mc.theWorld != null && mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
                mc.playerController.onPlayerDamageBlock(mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit);
                mc.thePlayer.swingItem();
            }
        }

        // Hareket kontrol
        long now = System.currentTimeMillis();
        if (now - lastMoveCheckTime >= 1000) { // 1 saniye geçti
            double currentX = mc.thePlayer.posX;
            double currentZ = mc.thePlayer.posZ;
            if (Math.abs(currentX - lastX) < 0.01 && Math.abs(currentZ - lastZ) < 0.01) {
                // Oyuncu hareket etmemiş
                releaseCurrentKey();
                nextState();
                holdCurrentKey();
            }
            lastX = currentX;
            lastZ = currentZ;
            lastMoveCheckTime = now;
        }
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText().toLowerCase();
        if (message.contains("reboot") || message.contains("void") || message.contains("limbo") || message.contains("visit")) {
            enabled = false;
            releaseAllKeys();
            mc.thePlayer.addChatMessage(new ChatComponentText("§c[KeyLoop] Otomasyon durduruldu(chat(özel kelime görüldü)"));
        }
    }

    private void holdCurrentKey() {
        KeyBinding key = getCurrentKeyBinding();
        if (key != null) KeyBinding.setKeyBindState(key.getKeyCode(), true);
    }

    private void releaseCurrentKey() {
        KeyBinding key = getCurrentKeyBinding();
        if (key != null) KeyBinding.setKeyBindState(key.getKeyCode(), false);
    }

    private void releaseAllKeys() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
    }

    private void nextState() {
        if (currentState == State.HOLD_A) currentState = State.HOLD_W;
        else if (currentState == State.HOLD_W) currentState = State.HOLD_D;
        else if (currentState == State.HOLD_D) {
            currentState = State.HOLD_W2;
        } else if (currentState == State.HOLD_W2) {
            currentState = State.HOLD_A;
        }

        // Tuş değiştiğinde kamerayı sabitle
        if (mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = 0;
            mc.thePlayer.rotationPitch = -58;
        }

    }

    private KeyBinding getCurrentKeyBinding() {
        if (currentState == State.HOLD_A) return mc.gameSettings.keyBindLeft;
        if (currentState == State.HOLD_W || currentState == State.HOLD_W2) return mc.gameSettings.keyBindForward;
        if (currentState == State.HOLD_D) return mc.gameSettings.keyBindRight;
        return null;
    }
}