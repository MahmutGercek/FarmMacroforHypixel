package com.example.keyloop;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class YawPitchGui extends GuiScreen {
    private GuiTextField yawField;
    private GuiTextField pitchField;
    private GuiButton confirmButton;
    private String errmsg = "";
    private final keyloop parent;

    public YawPitchGui(keyloop parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        int centerX = width / 2;
        int centerY = height / 2;

        yawField = new GuiTextField(0, fontRendererObj, centerX - 50, centerY - 30, 100, 20);
        pitchField = new GuiTextField(1, fontRendererObj, centerX - 50, centerY, 100, 20);

        yawField.setFocused(true);
        yawField.setText("Yaw");
        pitchField.setText("Pitch");

        buttonList.add(confirmButton = new GuiButton(2, centerX - 30, centerY + 40, 60, 20, "Confirm"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == confirmButton) {
            try {
                float yaw = Float.parseFloat(yawField.getText());
                float pitch = Float.parseFloat(pitchField.getText());
                parent.setTargetLook(yaw, pitch);
                mc.displayGuiScreen(null);
            } catch (NumberFormatException e) {
                errmsg = "Enter a valid number";
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
        yawField.textboxKeyTyped(typedChar, keyCode);
        pitchField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        yawField.mouseClicked(mouseX, mouseY, mouseButton);
        pitchField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Enter Yaw and Pitch", width / 2, height / 2 - 60, 0xFFFFFF);
        yawField.drawTextBox();
        pitchField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!errmsg.isEmpty()) {
            GuiButton confirmbutton = this.buttonList.get(0);
            int msgX = confirmbutton.xPosition + confirmbutton.width / 2;
            int msgY = confirmbutton.yPosition + confirmbutton.height + 5;
            drawCenteredString(this.fontRendererObj, errmsg, msgX, msgY, 0xFF5555);
        }
    }
}
