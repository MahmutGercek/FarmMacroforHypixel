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

@Mod(modid = "keyloopmod", name = "Key Loop Mod", version = "1.0", clientSideOnly = true)        //The name is a bit simple I know but it is what it is
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
        HOLD_A, HOLD_W, HOLD_D, HOLD_W2, HOLD_S, HOLD_W_AFTER_S
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    //Disabling the ESC GUI menu when ALT+TAB or when minecraft is in background
    public void onGuiOpen(GuiOpenEvent event) { 
        if (!enabled) return;
        if (event.gui instanceof net.minecraft.client.gui.GuiIngameMenu) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    //Assigning the button for our mod to start,you can change it to any button you want but I preffered right shift for this
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null) return;

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
        
        //Always swinging the item(mouse left click hold)
        if (!Mouse.isButtonDown(0)) {
            Mouse.poll();
            Mouse.next();
            if (mc.thePlayer != null && mc.theWorld != null && mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
                mc.playerController.onPlayerDamageBlock(mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit);
                mc.thePlayer.swingItem();
            }
        }
        //our movement loop depends on if the player doesnt change places for the past 1 second, if the case is true we switch to next button(A-W-D-W)
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
    //This place made for if someone visit your island or server resets and you got send to hub,the mod stops itself and lower the chance of you getting banned
    public void onChatMessage(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText().toLowerCase();
        if (message.contains("reboot") || message.contains("void") || message.contains("limbo") || message.contains("visit")) {
            enabled = false;
            releaseAllKeys();
            mc.thePlayer.addChatMessage(new ChatComponentText("(Stopped the operation,special word detected!)"));
        }
    }
    //This are holds our current button
    private void holdCurrentKey() {
        KeyBinding key = getCurrentKeyBinding();
        if (key != null) KeyBinding.setKeyBindState(key.getKeyCode(), true);
    }
    //This are releases our current button
    private void releaseCurrentKey() {
        KeyBinding key = getCurrentKeyBinding();
        if (key != null) KeyBinding.setKeyBindState(key.getKeyCode(), false);
    }
    //This are works when we close the mod,or a special case happens
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

        if (mc.thePlayer != null) {            // You need to reassign the coordinates for your own farm, mine suits with 0 to -58
            mc.thePlayer.rotationYaw = 0;
            mc.thePlayer.rotationPitch = -58;
        }
    }

    //We assign our buttons here
    private KeyBinding getCurrentKeyBinding() {
        if (currentState == LoopState.HOLD_A) return mc.gameSettings.keyBindLeft;
        if (currentState == LoopState.HOLD_W || currentState == LoopState.HOLD_W2 || currentState == LoopState.HOLD_W_AFTER_S)
            return mc.gameSettings.keyBindForward;
        if (currentState == LoopState.HOLD_D) return mc.gameSettings.keyBindRight;
        if (currentState == LoopState.HOLD_S) return mc.gameSettings.keyBindBack;
        return null;
    }
}
