package com.example.keyloop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
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
    private boolean wasshiftDown = false;
    private boolean shouldTapWOnce = false;

    private long lastMoveCheckTime = 0;
    private double lastX = 0;
    private double lastZ = 0;
    private int d_counter = 0;

    private LoopState currentState = LoopState.HOLD_A;
    private float targetYaw;
    private float targetPitch;

    private enum LoopState {
        HOLD_A, HOLD_W, HOLD_D, HOLD_W2, HOLD_S, HOLD_W_AFTER_S
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Register this class to listen to Forge events
        MinecraftForge.EVENT_BUS.register(this);
        System.out.println("[KeyLoopMod] Event handler registered.");
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
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        boolean GuiOpen = Keyboard.isKeyDown(Keyboard.KEY_O);
        if (GuiOpen) {
            mc.displayGuiScreen(new YawPitchGui(this));
        }
        boolean isKeyDown = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (isKeyDown && !wasshiftDown) {
            enabled = !enabled;
            if (!enabled) {
                releaseAllKeys();
            } else {
                mc.thePlayer.rotationYaw = targetYaw;
                mc.thePlayer.rotationPitch = targetPitch;
                currentState = LoopState.HOLD_A;
                holdCurrentKey();
                lastX = mc.thePlayer.posX;
                lastZ = mc.thePlayer.posZ;
                lastMoveCheckTime = System.currentTimeMillis();
            }
        }
        wasshiftDown = isKeyDown;

        if (!enabled) return;

        if (shouldTapWOnce) {
            shouldTapWOnce = false;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);

            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            }).start();
        }

        if (!Mouse.isButtonDown(0)) {
            Mouse.poll();
            Mouse.next();
            if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
                mc.playerController.onPlayerDamageBlock(mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit);
                mc.thePlayer.swingItem();
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastMoveCheckTime >= 1000) {
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
        if (message.contains("reboot") || message.contains("void") || message.contains("limbo") || message.contains("visit") || message.contains("evacuate")) {
            enabled = false;
            releaseAllKeys();
            mc.thePlayer.addChatMessage(new ChatComponentText("(Stopped the operation, special word detected!)"));
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
                currentState = LoopState.HOLD_W_AFTER_S;
                break;
            case HOLD_W_AFTER_S:
                currentState = LoopState.HOLD_A;
                break;
        }

        mc.thePlayer.rotationYaw = targetYaw;
        mc.thePlayer.rotationPitch = targetPitch;
    }

    private KeyBinding getCurrentKeyBinding() {
        switch (currentState) {
            case HOLD_A: return mc.gameSettings.keyBindLeft;
            case HOLD_W:
            case HOLD_W2:
            case HOLD_W_AFTER_S: return mc.gameSettings.keyBindForward;
            case HOLD_D: return mc.gameSettings.keyBindRight;
            case HOLD_S: return mc.gameSettings.keyBindBack;
            default: return null;
        }
    }

    public void setTargetLook(float yaw, float pitch) {
        this.targetYaw = yaw;
        this.targetPitch = pitch;
    }
}
