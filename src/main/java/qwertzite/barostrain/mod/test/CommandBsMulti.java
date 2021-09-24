package qwertzite.barostrain.mod.test;

import java.util.Random;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import qwertzite.barostrain.core.BsExplosions;
import qwertzite.barostrain.mod.BaroStrainExplosionCore;
import qwertzite.barostrain.mod.CommandExplosion;

public class CommandBsMulti extends CommandBase {

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
		
		if (s < 0) return;
		double x = Math.min(x1, x2);
		double z = Math.min(z1, z2);
		double dx = Math.abs(x1 - x2);
		double dz = Math.abs(z1 - z2);
		long nn = Math.round(dx*dz*p);
		Random rand = new Random();
		World world = sender.getEntityWorld();
		for (long n = 0;n < nn; n++) {
			double xi = x + rand.nextDouble()*dx;
			double zi = z + rand.nextDouble()*dz;
			double yi = world.getHeight(MathHelper.floor(xi), MathHelper.floor(zi)) + h;
			BaroStrainExplosionCore.INSTANCE.scheduler.add(n*2, () -> BsExplosions.explode(world, null, xi, yi, zi, (float) s));
		}
		BaroStrainExplosionCore.INSTANCE.scheduler.add(nn*2, () -> sender.sendMessage(new TextComponentString(String.format("Executed " + (nn) + " explosions."))));
		sender.sendMessage(new TextComponentString(String.format("Executed " + nn + " explosions.")));
		
	}

}
