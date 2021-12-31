package qwertzite.barostrain.core.common.coord;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import qwertzite.barostrain.core.fem.CoordHelper;

public class VertexPos {
	
	private final BlockPos pos;
	private final Vec3i shift;
	
	public VertexPos(BlockPos pos, Vec3i shift) {
		this.pos = pos;
		this.shift = shift;
	}
	
	public BlockPos[] getBelongingElements() {
		return CoordHelper.memberElements(this);
	}
	
	public BlockPos getPos() { return this.pos; }
	
	@Override
	public String toString() {
		return "[" + pos + ":" + (shift==null ? "*" : shift);
	}
	
	@Override
	public int hashCode() {
		return this.pos.hashCode() * 17 + this.shift.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VertexPos) {
			VertexPos other = (VertexPos) obj;
			return this.pos.equals(other.pos) && this.shift.equals(other.shift);
		}
		return false;
	}
}
