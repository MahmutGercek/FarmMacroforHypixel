package com.example.keyloop;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;

public class YawPitchGui extends GuiScreen {
    private GuiTextField yawField;
    private GuiTextField pitchField;
    private GuiButton confirmButton;

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
                mc.displayGuiScreen(null); // Close GUI
            } catch (NumberFormatException e) {
                // Invalid input
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
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
    }
}
