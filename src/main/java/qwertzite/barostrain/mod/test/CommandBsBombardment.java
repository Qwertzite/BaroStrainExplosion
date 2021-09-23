package qwertzite.barostrain.mod.test;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.util.text.TextComponentString;
import qwertzite.barostrain.core.BsExplosions;
import qwertzite.barostrain.mod.CommandExplosion;

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
		return "/bsMulti x1 z1 x2 z2 h p s\n"
				+ "h: margin from surface\n"
				+ "p: probability per 1 m^2. clamped to [0, inf)\n"
				+ "s: strength";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		// TODO Auto-generated method stub
		double x1 = CommandExplosion.parseDouble(args[0]);
		double z1 = CommandExplosion.parseDouble(args[1]);
		double x2 = CommandExplosion.parseDouble(args[2]);
		double z2 = CommandExplosion.parseDouble(args[3]);
		double h = CommandExplosion.parseDouble(args[4]);
		double p = CommandExplosion.parseDouble(args[5]);
		double s = CommandExplosion.parseDouble(args[6]);
		sender.sendMessage(new TextComponentString(String.format("Executing multiple explosions.")));
		
		
//		BsExplosions.explode(sender.getEntityWorld(), null, x, y, z, (float) s);
		
	}

}
