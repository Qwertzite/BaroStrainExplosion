package qwertzite.barostrain.core.common.coord;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class BlockFace {
	private final BlockPos blockpos;
	private final EnumFacing facing;
	
	public BlockFace(BlockPos pos, EnumFacing face) {
		this.blockpos = pos;
		this.facing = face;
	}

	public BlockPos getBlockpos() {
		return blockpos;
	}

	public EnumFacing getFacing() {
		return facing;
	}
	
	@Override
	public int hashCode() {
		return this.blockpos.hashCode() * 17 + facing.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) { return true; }
		if (obj instanceof BlockFace) {
			BlockFace other = (BlockFace) obj;
			return other.getBlockpos().equals(this.getBlockpos()) && 
					other.getFacing().equals(this.getFacing());
		}else {
			return false;
		}
	}
}
