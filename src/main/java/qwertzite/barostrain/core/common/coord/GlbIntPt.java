package qwertzite.barostrain.core.common.coord;

import javax.annotation.concurrent.Immutable;

import net.minecraft.util.math.BlockPos;

@Immutable
public class GlbIntPt {
	private final BlockPos pos;
	private final IntPoint point;
	public GlbIntPt(BlockPos pos, IntPoint point) {
		this.pos = pos;
		this.point = point;
	}
	public BlockPos getPos() {
		return pos;
	}
	public IntPoint getPoint() {
		return point;
	}
	
	@Override
	protected GlbIntPt clone() {
		return new GlbIntPt(this.getPos(), this.getPoint());
	}
	@Override
	public int hashCode() {
		return this.getPos().hashCode() * 31 + this.getPoint().hashCode();
	}
	@Override
	public String toString() {
		return "int" + this.getPos() + ":" + this.getPoint();
	}
}
