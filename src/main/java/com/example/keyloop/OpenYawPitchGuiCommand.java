package com.example.keyloop;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class OpenYawPitchGuiCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "yawpitch";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/yawpitch - Opens the yaw/pitch input GUI";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft.getMinecraft().displayGuiScreen(new YawPitchGui(keyloop.INSTANCE));
        sender.addChatMessage(new ChatComponentText("Yaw/Pitch GUI opened."));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Everyone can use it
    }
}
