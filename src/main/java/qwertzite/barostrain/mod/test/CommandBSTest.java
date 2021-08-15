package qwertzite.barostrain.mod.test;

import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import qwertzite.barostrain.core.AxisStrain;

public class CommandBSTest extends CommandBase {

	@Override
	public String getName() {
		return "bsTest";
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
		return "/bsTest <strength> [x] [y] [z] [face/dir]\n"
				+ "face: the face to apply force {up, down, east, west, south, north}\n"
				+ "dir: the direction of applied force. { x+ x- y+ y- z+ z- }";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		double strength = Double.parseDouble(args[0]);
		int posx = 0;
		int posy = 0;
		int posz = 0;
		EnumFacing face = EnumFacing.WEST; // x+
		if (args.length >= 5) {
			posx = Integer.parseInt(args[1]);
			posy = Integer.parseInt(args[2]);
			posz = Integer.parseInt(args[3]);
			face = EnumFacing.byName(args[4]);
			if (face == null) {
				String ds = args[4].toLowerCase();
				AxisDirection dir = ds.contains("+") ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE;
				Axis axis;
				if (ds.contains("x")) axis = Axis.X;
				else if (ds.contains("y")) axis = Axis.Y;
				else axis = Axis.Z;
				face = EnumFacing.getFacingFromAxis(dir, axis);
			}
		}
		this.executeExplosion(strength, new BlockPos(posx, posy, posz), face, sender.getEntityWorld());
	}
	
	private void executeExplosion(double strength, BlockPos pos, EnumFacing face, World world) {
		DummyStrainSimulator sim = new DummyStrainSimulator(world);
		AxisStrain axis = new AxisStrain(sim, face.getAxis());
		
		axis.applyForce(null, pos, face, strength);
		Set<BlockPos> affected = axis.evaluate();
		for (BlockPos blockpos : affected) {
			IBlockState iblockstate = world.getBlockState(blockpos);
			Block block = iblockstate.getBlock();
			if (iblockstate.getMaterial() != Material.AIR) {
				if (block.canDropFromExplosion(null)) {
					block.dropBlockAsItemWithChance(world, blockpos, world.getBlockState(blockpos),
							1.0F / (float)strength, 0);
				}
				block.onBlockExploded(world, blockpos, null);
			}
		}
		world.playSound((EntityPlayer) null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE,
				SoundCategory.BLOCKS, 4.0F,
				(1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
	}

}
