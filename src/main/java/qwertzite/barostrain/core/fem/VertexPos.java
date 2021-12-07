package qwertzite.barostrain.core.fem;

import java.util.Objects;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class VertexPos {
	
	private final BlockPos pos;
	private final Vec3i shift;
	
	public VertexPos(BlockPos pos, Vec3i shift) {
		this.pos = pos;
		this.shift = shift;
	}
	
	public VertexPos(BlockPos pos, ElemVertex ev) {
		int dx1 = ev.getA() / 2;
		int sx1 = ev.getA() % 2;
		int dx2 = ev.getB() / 2;
		int sx2 = ev.getB() % 2;
		int dx3 = ev.getC() / 2;
		int sx3 = ev.getC() % 2;
		this.pos = pos.add(dx1, dx2, dx3);
		this.shift = new Vec3i(sx1, sx2, sx3);
	}
	
	public static VertexPos[] fromElementPos(BlockPos element) {
		final int NV = ElemVertex.values().length;
		VertexPos[] pos = new VertexPos[NV];
		for (int i = 0; i < NV; i++) {
			pos[i] = new VertexPos(element, ElemVertex.values()[i]);
		}
		return pos;
	}
	
	@Override
	public String toString() {
		return "[" + pos + ":" + (shift==null ? "*" : shift);
	}
	
	@Override
	public int hashCode() {
		return this.pos.hashCode() * 17 + Objects.hashCode(shift);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VertexPos) {
			VertexPos other = (VertexPos) obj;
			return this.pos.equals(other.pos) && this.shift == other.shift;
		}
		return false;
	}
}
