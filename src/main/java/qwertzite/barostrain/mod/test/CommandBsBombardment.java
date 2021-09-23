package qwertzite.barostrain.mod.test;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.CommandBlockBaseLogic;

public class CommandBsBombardment extends CommandBase {

	@Override
	public String getName() {
		return "bsMulti";
	}
	
	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		if (sender instanceof CommandBlockBaseLogic) {
			return sender.canUseCommand(0, this.getName());
		}
		return sender.canUseCommand(this.getRequiredPermissionLevel(), this.getName());
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/bsMulti x1 z1 x2 z2 p s";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		// TODO Auto-generated method stub
		
	}

}
