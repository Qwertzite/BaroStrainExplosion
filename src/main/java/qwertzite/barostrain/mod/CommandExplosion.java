package qwertzite.barostrain.mod;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.util.text.TextComponentString;
import qwertzite.barostrain.core.BSExplosionBase;
import qwertzite.barostrain.core.BsExplosions;

public class CommandExplosion extends CommandBase {

	@Override
	public String getName() {
		return "bsExp";
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
		return "/bsExp <type:v(Vanilla),bs(BaroStrain)> <x> <y> <z> <strength>";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		BSExplosionBase.PressureRay.RAYS.clear();
		switch (args[0].toLowerCase()) {
		case "v":
		case "vanilla":
			this.vanilla(server, sender, args);
			break;
		case "bs":
		case "barostrain":
			this.barostrain(server, sender, args);
			break;
		default:
			sender.sendMessage(new TextComponentString(String.format("Unknown explosion type! %s\nAllowed types are 'vanilla' and 'barostrain'.", args[0])));
		}
	}
	
	private void vanilla(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		double x = CommandExplosion.parseDouble(args[1]);
		double y = CommandExplosion.parseDouble(args[2]);
		double z = CommandExplosion.parseDouble(args[3]);
		double s = CommandExplosion.parseDouble(args[4]);
		sender.getEntityWorld().newExplosion(null, x, y, z, (float) s, false, true);
	}
	
	private void barostrain(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		double x = CommandExplosion.parseDouble(args[1]);
		double y = CommandExplosion.parseDouble(args[2]);
		double z = CommandExplosion.parseDouble(args[3]);
		double s = CommandExplosion.parseDouble(args[4]);
		sender.sendMessage(new TextComponentString(String.format("Causing an explosion at %f, %f, %f with strength %f.", x, y, z, s)));
		BsExplosions.explode(sender.getEntityWorld(), null, x, y, z, (float) s);
	}
}