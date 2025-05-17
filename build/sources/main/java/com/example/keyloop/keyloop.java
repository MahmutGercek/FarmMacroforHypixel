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
import java.io.*;

@Mod(modid = "keyloopmod", name = "Key Loop Mod", version = "1.0", clientSideOnly = true)
public class keyloop {

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;
    private boolean WasShiftDown = false;
    private static final File configFile = new File(Minecraft.getMinecraft().mcDataDir, "keyloop_config.txt");
    private long lastMoveCheckTime = 0;
    private double lastX = 0;
    private double lastZ = 0;
    private int d_counter = 0;
    private int sTickCounter = 0;
    private boolean tempWActive = false;
    private long tempWStartTime = 0;


    private LoopState currentState = LoopState.HOLD_A;
    private float targetYaw;
    private float targetPitch;

    private enum LoopState {
        HOLD_A, HOLD_W, HOLD_D, HOLD_W2, HOLD_S
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        loadConfig();
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
        if (mc.thePlayer == null || mc.currentScreen != null) return;

        boolean GuiOpen = Keyboard.isKeyDown(Keyboard.KEY_O);
        if (GuiOpen) {
            mc.displayGuiScreen(new YawPitchGui(this));
        }

        boolean isKeyDown = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (isKeyDown && !WasShiftDown) {
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
        WasShiftDown = isKeyDown;

        if (!enabled) return;

        if (tempWActive) {
            long now = System.currentTimeMillis();
            if (now - tempWStartTime < 500) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            } else {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                tempWActive = false;
            }
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
        if (now - lastMoveCheckTime >= 400) {
            if (currentState == LoopState.HOLD_S) {
                sTickCounter++;
                if (sTickCounter >= 70) {
                    releaseCurrentKey();
                    nextState();
                    holdCurrentKey();
                    sTickCounter = 0;
                    lastMoveCheckTime = now;
                }
                return;
            }
            else {
                sTickCounter = 0;
            }
            double currentX = mc.thePlayer.posX;
            double currentZ = mc.thePlayer.posZ;
            double dx = currentX - lastX;
            double dz = currentZ - lastZ;
            double distance = Math.sqrt(dx * dx + dz * dz);

            if (distance < 1.6) {
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
        if (key != null) {
            KeyBinding.setKeyBindState(key.getKeyCode(), true);
        }
    }
    private void releaseCurrentKey() {
        KeyBinding key = getCurrentKeyBinding();
        if (key != null) {
            KeyBinding.setKeyBindState(key.getKeyCode(), false);
        }
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
                tempWActive = true;
                tempWStartTime = System.currentTimeMillis();
                break;
        }

        mc.thePlayer.rotationYaw = targetYaw;
        mc.thePlayer.rotationPitch = targetPitch;
    }

    private KeyBinding getCurrentKeyBinding() {
        switch (currentState) {
            case HOLD_A: return mc.gameSettings.keyBindLeft;
            case HOLD_W: return mc.gameSettings.keyBindForward;
            case HOLD_W2: return mc.gameSettings.keyBindForward;
            case HOLD_D: return mc.gameSettings.keyBindRight;
            case HOLD_S: return mc.gameSettings.keyBindBack;
            default: return null;
        }
    }

    public void setTargetLook(float yaw, float pitch) {
        this.targetYaw = yaw;
        this.targetPitch = pitch;
    }
    public float getTargetYaw() {
        return targetYaw;
    }

    public float getTargetPitch() {
        return targetPitch;
    }
    public void saveConfig() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
            writer.write(Float.toString(this.targetYaw));
            writer.newLine();
            writer.write(Float.toString(this.targetPitch));
            writer.close();
        } catch (Exception e) {
            System.out.println("Failed to save config: " + e.getMessage());
        }
    }
    private void loadConfig() {
        if (!configFile.exists()) return;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            this.targetYaw = Float.parseFloat(reader.readLine());
            this.targetPitch = Float.parseFloat(reader.readLine());
            reader.close();
        } catch (Exception e) {
            System.out.println("Failed to load config: " + e.getMessage());
        }
    }
}
