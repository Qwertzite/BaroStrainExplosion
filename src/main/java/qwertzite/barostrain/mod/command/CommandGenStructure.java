package qwertzite.barostrain.mod.command;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

public class CommandGenStructure extends CommandBase {

	@Override
	public String getName() {
		return "bsStructure";
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
		return "/bsStructure";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		/*
		 * name
		 * pos x y z
		 * rot= 0 90 180 270
		 * mirror= | ^ <>
		 */
		final String name = args[0];
		final double x = CommandExplosion.parseDouble(args[1]);
		final double y = CommandExplosion.parseDouble(args[2]);
		final double z = CommandExplosion.parseDouble(args[3]);
		
		String result = this.load(name, new BlockPos(x, y, z), sender.getEntityWorld());
		sender.sendMessage(new TextComponentString(String.format(result)));
	}
	
	private String load(String name, BlockPos pos, World world) {

		WorldServer worldserver = (WorldServer) world;
		MinecraftServer minecraftserver = world.getMinecraftServer();
		TemplateManager templatemanager = worldserver.getStructureTemplateManager();
		Template template = templatemanager.get(minecraftserver, new ResourceLocation(name));

		if (template == null) { return "Unknown template! " + name; }

		PlacementSettings placementsettings = (new PlacementSettings())
//				.setMirror(this.mirror)
//				.setRotation(this.rotation)
				.setIgnoreEntities(false).setChunk((ChunkPos) null)
				.setReplacedBlock((Block) null).setIgnoreStructureBlock(false);

//		if (this.integrity < 1.0F) {
//			placementsettings.setIntegrity(MathHelper.clamp(this.integrity, 0.0F, 1.0F))
//					.setSeed(Long.valueOf(this.seed));
//		}

		template.addBlocksToWorldChunk(world, pos, placementsettings);
		return "Generated " + name + " at " + pos;
	}

}
