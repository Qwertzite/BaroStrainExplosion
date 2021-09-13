package qwertzite.barostrain.mod.test;

import java.util.HashMap;
import java.util.Map;
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
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
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
		return "/bsTest <type> <args...>\n"
				+ "type: {n d}\n"
				+ "  type n: <strength> [x] [y] [z] [face/dir]\n"
				+ "    face: the face to apply force {up, down, east, west, south, north}\n"
				+ "    dir: the direction of applied force. { x+ x- y+ y- z+ z- }\n"
				+ "  type d: {x1} {y1} {z1} {s1} {d1} {x2} {y2} {z2} {s2} {d2}\n"
				+ "    xn, yn, zn: position x, y, z\n"
				+ "    sn: strength\n"
				+ "    dn: the direction of applied force. overrides fn. { x+ x- y+ y- z+ z- }\n"
				+ "      d1 will be used as the evaluating axis.";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		switch (args[0]) {
		case "n": this.executeNormal(sender, args); break;
		case "d": this.executeDouble(sender, args); break;
		default:
			sender.sendMessage(new TextComponentString(String.format("Unknown type %s. See help for usage.", args[0])));
		}
	}
	
	private void executeNormal(ICommandSender sender, String[] args) {
		double strength = Double.parseDouble(args[1]);
		int posx = 0;
		int posy = 0;
		int posz = 0;
		EnumFacing face = EnumFacing.WEST; // x+
		if (args.length >= 6) {
			posx = Integer.parseInt(args[2]);
			posy = Integer.parseInt(args[3]);
			posz = Integer.parseInt(args[4]);
			face = EnumFacing.byName(args[5]);
			if (face == null) {
				String ds = args[5].toLowerCase();
				AxisDirection dir = ds.contains("+") ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE;
				Axis axis;
				if (ds.contains("x")) axis = Axis.X;
				else if (ds.contains("y")) axis = Axis.Y;
				else axis = Axis.Z;
				face = EnumFacing.getFacingFromAxis(dir, axis);
			}
		}
		sender.sendMessage(new TextComponentString(String.format("Strain testexecuted at (%d, %d, %d), strength %f, on face %s",
				posx, posy, posz, strength, face.toString())));
		this.executeExplosion(strength, new BlockPos(posx, posy, posz), face, sender.getEntityWorld());
	}
	
	private void executeExplosion(double strength, BlockPos pos, EnumFacing face, World world) {
		DummyStrainSimulator sim = new DummyStrainSimulator(world);
		AxisStrain axis = new AxisStrain(sim, face.getAxis());
		
		axis.applyForce(null, pos, face, strength);
		
		Set<BlockPos> affected = axis.evaluate();
		this.destroyBlocks(affected, world, strength);
		world.playSound((EntityPlayer) null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE,
				SoundCategory.BLOCKS, 4.0F,
				(1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
	}

	private void executeDouble(ICommandSender sender, String[] args) {
		Map<String, String> arg = new HashMap<>();
		for (String s : args) {
			String[] ss = s.split("=", 2);
			if (ss.length >= 2) arg.put(ss[0], ss[1]);
		}
		int x1 = arg.containsKey("x1") ? Integer.parseInt(arg.get("x1")) : 0;
		int y1 = arg.containsKey("y1") ? Integer.parseInt(arg.get("y1")) : 0;
		int z1 = arg.containsKey("z1") ? Integer.parseInt(arg.get("z1")) : 0;
		double s1 = arg.containsKey("s1") ? Double.parseDouble(arg.get("s1")) : 0;
		EnumFacing face1 = arg.containsKey("f1") ? EnumFacing.byName(arg.get("f1")) : EnumFacing.WEST;
		face1 = arg.containsKey("d1") ? strDirToFace(arg.get("d1")) : face1;
		
		int x2 = arg.containsKey("x2") ? Integer.parseInt(arg.get("x2")) : 0;
		int y2 = arg.containsKey("y2") ? Integer.parseInt(arg.get("y2")) : 0;
		int z2 = arg.containsKey("z2") ? Integer.parseInt(arg.get("z2")) : 0;
		double s2 = arg.containsKey("s2") ? Double.parseDouble(arg.get("s2")) : 0;
		EnumFacing face2 = arg.containsKey("f2") ? EnumFacing.byName(arg.get("f2")) : EnumFacing.EAST;
		face2 = arg.containsKey("d2") ? strDirToFace(arg.get("d2")) : face2;
		
		World world = sender.getEntityWorld();
		DummyStrainSimulator sim = new DummyStrainSimulator(world);
		AxisStrain axis = new AxisStrain(sim, face1.getAxis());
		
		axis.applyForce(null, new BlockPos(x1, y1,z1), face1, s1);
		axis.applyForce(null, new BlockPos(x2, y2,z2), face2, s2);
		
		Set<BlockPos> affected = axis.evaluate();
		this.destroyBlocks(affected, world, (s1 + s2) / 2);
		world.playSound((EntityPlayer) null, x1, y1, z1, SoundEvents.ENTITY_GENERIC_EXPLODE,
				SoundCategory.BLOCKS, 4.0F,
				(1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
		
	}
	
	private EnumFacing strDirToFace(String dirs) {
		String ds = dirs.toLowerCase();
		AxisDirection dir = ds.contains("+") ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE;
		Axis axis;
		if (ds.contains("x")) axis = Axis.X;
		else if (ds.contains("y")) axis = Axis.Y;
		else axis = Axis.Z;
		return EnumFacing.getFacingFromAxis(dir, axis);
	}
	
	private void destroyBlocks(Set<BlockPos> affected, World world, double strength) {
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
	}
}
