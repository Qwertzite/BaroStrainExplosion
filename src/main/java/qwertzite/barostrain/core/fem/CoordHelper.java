package qwertzite.barostrain.core.fem;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import qwertzite.barostrain.core.common.coord.BlockFace;
import qwertzite.barostrain.core.common.coord.ElemVertex;
import qwertzite.barostrain.core.common.coord.VertexPos;

public class CoordHelper {
	
	public static VertexPos vertexPos(BlockPos pos, ElemVertex ev) {
		int dx1 = (ev.getA()+1) / 2;
		int sx1 = (ev.getA()+1) % 2;
		int dx2 = (ev.getB()+1) / 2;
		int sx2 = (ev.getB()+1) % 2;
		int dx3 = (ev.getC()+1) / 2;
		int sx3 = (ev.getC()+1) % 2;
		pos = pos.add(dx1, dx2, dx3);
		Vec3i shift = new Vec3i(sx1, sx2, sx3);
		return new VertexPos(pos, shift);
	}
	
	/** Sorted by element vertex index. */
	public static VertexPos[] memberVertexPos(BlockPos element) {
		final int NV = ElemVertex.values().length;
		VertexPos[] pos = new VertexPos[NV];
		for (ElemVertex ev : ElemVertex.values()) {
			pos[ev.getIndex()] = vertexPos(element, ev);
		}
		return pos;
	}
	
	public static VertexPos[] memberVertexPos(BlockFace blockface) {
		BlockPos pos = blockface.getBlockpos();
		ElemVertex[] elemVertex = memberElemVertex(blockface.getFacing());
		VertexPos[] vertexPos = new VertexPos[elemVertex.length];
		for (int i = 0; i < elemVertex.length; i++) { vertexPos[i] = vertexPos(pos, elemVertex[i]); }
		return vertexPos;
	}
	
	public static BlockPos[] memberElements(VertexPos glbVertex) {
		BlockPos pos = glbVertex.getPos();
		return new BlockPos[] {
				pos.add( 0,  0,  0), pos.add(-1,  0,  0), pos.add( 0, -1,  0), pos.add(-1, -1,  0),
				pos.add( 0,  0, -1), pos.add(-1,  0, -1), pos.add( 0, -1, -1), pos.add(-1, -1, -1)
		};
	}
	
	public static ElemVertex[] memberElemVertex(EnumFacing face) {
		switch (face) {
		case DOWN: // -y axis
			return new ElemVertex[] { ElemVertex.VNNN, ElemVertex.VPNN, ElemVertex.VNNP, ElemVertex.VPNP };
		case UP: // +y axis
			return new ElemVertex[] { ElemVertex.VNPN, ElemVertex.VPPN, ElemVertex.VNPP, ElemVertex.VPPP };
		case WEST: // -x axis
			return new ElemVertex[] { ElemVertex.VNNN, ElemVertex.VNNP, ElemVertex.VNPN, ElemVertex.VNPP };
		case EAST: // +x axis
			return new ElemVertex[] { ElemVertex.VPNN, ElemVertex.VPNP, ElemVertex.VPPN, ElemVertex.VPPP };
		case NORTH:
			return new ElemVertex[] { ElemVertex.VNNN, ElemVertex.VPNN, ElemVertex.VNPN, ElemVertex.VPPN };
		case SOUTH:
			return new ElemVertex[] { ElemVertex.VNNP, ElemVertex.VPNP, ElemVertex.VNPP, ElemVertex.VPPP };
		default:
			assert(false);
			return null;
		}
	}
}
