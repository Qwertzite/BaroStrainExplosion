package qwertzite.barostrain.mod.test;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import qwertzite.barostrain.core.BlockStrainSimulator;

public class DummyStrainSimulator extends BlockStrainSimulator {
	
	private World world;
	
	public DummyStrainSimulator(World world) {
		super(world, null, null);
		this.world = world;
	}
	
	@Override
	public synchronized double getBlockResistanceAt(BlockPos pos) {
		IBlockState iblockstate = this.world.getBlockState(pos);
		return iblockstate.getBlock().getExplosionResistance(this.world, pos, (Entity) null, null);
	}
	
	@Override
	public double getBlockHardnessAt(BlockPos pos) {
		return this.world.getBlockState(pos).getBlockHardness(this.world, pos);
	}

}
