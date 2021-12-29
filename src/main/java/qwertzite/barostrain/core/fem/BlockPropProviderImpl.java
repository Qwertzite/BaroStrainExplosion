package qwertzite.barostrain.core.fem;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class BlockPropProviderImpl extends AbstractBlockPropProvider {
	
	private final Explosion explosion;
	private final World world;
	private final Entity exploder;
	
	public BlockPropProviderImpl(Explosion explosion, Entity exploder, World world) {
		this.explosion = explosion;
		this.world = world;
		this.exploder = exploder;
	}
	
	@Override
	protected double hardness(BlockPos pos) {
		if (this.isDestoryed(pos)) return 0.0d;
		else return this.world.getBlockState(pos).getBlockHardness(this.world, pos);
	}

	@Override
	protected double resistance(BlockPos pos) {
		if (this.isDestoryed(pos)) return 0.0d; // 破壊判定されている場合
		IBlockState iblockstate = this.world.getBlockState(pos);
		double resistance = this.exploder != null
				? this.exploder.getExplosionResistance(this.explosion, this.world, pos, iblockstate)
				: iblockstate.getBlock().getExplosionResistance(this.world, pos, (Entity) null, this.explosion);
		return resistance;
	}
}
