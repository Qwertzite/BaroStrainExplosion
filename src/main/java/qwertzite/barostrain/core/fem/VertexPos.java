package qwertzite.barostrain.core.fem;

import java.util.Objects;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import qwertzite.barostrain.core.common.BlockFace;

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
		for (ElemVertex ev : ElemVertex.values()) {
			pos[ev.getIndex()] = new VertexPos(element, ev);
		}
		return pos;
	}
	
	public static VertexPos[] fromBlockFace(BlockFace blockface) {
		BlockPos pos = blockface.getBlockpos();
		switch (blockface.getFacing()) {
		case DOWN: // -y axis
			return new VertexPos[] {
					new VertexPos(pos, ElemVertex.VNNN), new VertexPos(pos, ElemVertex.VPNN),
					new VertexPos(pos, ElemVertex.VNNP), new VertexPos(pos, ElemVertex.VPNP)};
		case UP: // +y axis
			return new VertexPos[] {
					new VertexPos(pos, ElemVertex.VNPN), new VertexPos(pos, ElemVertex.VPPN),
					new VertexPos(pos, ElemVertex.VNPP), new VertexPos(pos, ElemVertex.VPPP)};
		case WEST: // -x axis
			return new VertexPos[] {
					new VertexPos(pos, ElemVertex.VNNN), new VertexPos(pos, ElemVertex.VNNP),
					new VertexPos(pos, ElemVertex.VNPN), new VertexPos(pos, ElemVertex.VNPP)};
		case EAST: // +x axis
			return new VertexPos[] {
					new VertexPos(pos, ElemVertex.VPNN), new VertexPos(pos, ElemVertex.VPNP),
					new VertexPos(pos, ElemVertex.VPPN), new VertexPos(pos, ElemVertex.VPPP)};
		case NORTH:
			return new VertexPos[] {
					new VertexPos(pos, ElemVertex.VNNN), new VertexPos(pos, ElemVertex.VPNN),
					new VertexPos(pos, ElemVertex.VNPN), new VertexPos(pos, ElemVertex.VPPN)};
		case SOUTH:
			return new VertexPos[] {
					new VertexPos(pos, ElemVertex.VNNP), new VertexPos(pos, ElemVertex.VPNP),
					new VertexPos(pos, ElemVertex.VNPP), new VertexPos(pos, ElemVertex.VPPP)};
		default:
			assert(false);
			return null;
		}
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
			return this.pos.equals(other.pos) && this.shift.equals(other.shift);
		}
		return false;
	}
}
