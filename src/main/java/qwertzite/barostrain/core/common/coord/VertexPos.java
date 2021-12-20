package qwertzite.barostrain.core.common.coord;

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
		for (ElemVertex ev : ElemVertex.values()) {
			pos[ev.getIndex()] = new VertexPos(element, ev);
		}
		return pos;
	}
	
	public static VertexPos[] fromBlockFace(BlockFace blockface) {
		BlockPos pos = blockface.getBlockpos();
		ElemVertex[] elemVertex = ElemVertex.getElemVertexForFace(blockface.getFacing());
		VertexPos[] vertexPos = new VertexPos[elemVertex.length];
		for (int i = 0; i < elemVertex.length; i++) { vertexPos[i] = new VertexPos(pos, elemVertex[i]); }
		return vertexPos;
	}
	
	public BlockPos[] getBelongingElements() {
		return new BlockPos[] {
				this.pos.add( 0,  0,  0), this.pos.add(-1,  0,  0), this.pos.add( 0, -1,  0), this.pos.add(-1, -1,  0),
				this.pos.add( 0,  0, -1), this.pos.add(-1,  0, -1), this.pos.add( 0, -1, -1), this.pos.add(-1, -1, -1)
		};
		
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
