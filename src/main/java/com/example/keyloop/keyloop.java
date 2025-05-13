package com.example.keyloop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Mod(modid = "keyloopmod", name = "Key Loop Mod", version = "1.0", clientSideOnly = true)
public class keyloop {
    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;
    private boolean wasShiftDown = false;

    private long lastMoveCheckTime = 0;
    private double lastX = 0;
    private double lastZ = 0;
    private int d_counter = 0;

    private LoopState currentState = LoopState.HOLD_A;

    private enum LoopState {
        HOLD_A, HOLD_W, HOLD_D, HOLD_W2,HOLD_S
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!enabled) return;
        if (event.gui instanceof net.minecraft.client.gui.GuiIngameMenu) {
            event.setCanceled(true);
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
                currentState = LoopState.HOLD_A;
                holdCurrentKey();
                lastX = mc.thePlayer.posX;
                lastZ = mc.thePlayer.posZ;
                lastMoveCheckTime = System.currentTimeMillis();
            }
        }
        wasShiftDown = isShiftDown;

        if (!enabled) return;

        // Mouse left click automation
        if (!Mouse.isButtonDown(0)) {
            Mouse.poll();
            Mouse.next();
            if (mc.thePlayer != null && mc.theWorld != null && mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
                mc.playerController.onPlayerDamageBlock(mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit);
                mc.thePlayer.swingItem();
            }
        }

        // Main A-W-D-W loop
        long now = System.currentTimeMillis();
        if (now - lastMoveCheckTime >= 1000) { // 1 second
            double currentX = mc.thePlayer.posX;
            double currentZ = mc.thePlayer.posZ;
            if (Math.abs(currentX - lastX) < 0.01 && Math.abs(currentZ - lastZ) < 0.01) {
                releaseCurrentKey();
                nextState();
                holdCurrentKey();
                lastX = mc.thePlayer.posX;
                lastZ = mc.thePlayer.posZ;
                lastMoveCheckTime = now;
            } else {
                lastX = currentX;
                lastZ = currentZ;
                lastMoveCheckTime = now;
            }
        }
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText().toLowerCase();
        if (message.contains("reboot") || message.contains("void") || message.contains("limbo") || message.contains("visit")) {
            enabled = false;
            releaseAllKeys();
            mc.thePlayer.addChatMessage(new ChatComponentText("(Stopped the operation,special word detected!)"));
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
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
    }

    private void nextState() {
        switch (currentState) {
            case HOLD_A:
                currentState = LoopState.HOLD_W;
                break;
            case HOLD_W:
                currentState = LoopState.HOLD_D;
                break;
            case HOLD_D:
                d_counter++;
                if (d_counter >= 5) {
                    currentState = LoopState.HOLD_S;
                    d_counter = 0;
                } else {
                    currentState = LoopState.HOLD_W2;
                }
                break;
            case HOLD_W2:
                currentState = LoopState.HOLD_A;
                break;
            case HOLD_S:
                currentState = LoopState.HOLD_A;
                break;
        }

        // Yön ayarlaması
        if (mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = 0;
            mc.thePlayer.rotationPitch = -58;
        }
    }


    private KeyBinding getCurrentKeyBinding() {
        if (currentState == LoopState.HOLD_A) return mc.gameSettings.keyBindLeft;
        if (currentState == LoopState.HOLD_W || currentState == LoopState.HOLD_W2) return mc.gameSettings.keyBindForward;
        if (currentState == LoopState.HOLD_D) return mc.gameSettings.keyBindRight;
        if (currentState == LoopState.HOLD_S) return mc.gameSettings.keyBindBack;
        return null;
    }
}