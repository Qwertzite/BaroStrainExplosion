package qwertzite.barostrain.core.common.coord;

import net.minecraft.util.EnumFacing;

public enum ElemVertex {
	// class 0
	VNNN(0, 0, -1, -1, -1),
	VPNN(1, 0,  1, -1, -1),
	VNPN(2, 0, -1,  1, -1),
	VPPN(3, 0,  1,  1, -1),
	VNNP(4, 0, -1, -1,  1),
	VPNP(5, 0,  1, -1,  1),
	VNPP(6, 0, -1,  1,  1),
	VPPP(7, 0,  1,  1,  1),
	// class 1
//	V0NP(0,  0, -1,  1),
//	V0NN(0,  0, -1, -1),
//	VPN0(0,  1, -1,  0),
//	VNN0(0, -1, -1,  0),
//	VP0P(0,  1,  0,  1),
//	VN0P(0, -1,  0,  1),
//	VP0N(0,  1,  0, -1),
//	VN0N(0, -1,  0, -1),
//	V0PP(0,  0,  1,  1),
//	V0PN(0,  0,  1, -1),
//	VPP0(0,  1,  1,  0),
//	VNP0(0, -1,  1,  0),
	;
	
	private final int index;
	private final int rank;
	private final int a;
	private final int b;
	private final int c;
	private ElemVertex(int index, int rank, int a, int b, int c) {
		this.index = index;
		this.rank = rank;
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	public static ElemVertex[] getElemVertexForFace(EnumFacing face) {
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
	
	public int getIndex() {
		return index;
	}
	public int getRank() {
		return rank;
	}
	public int getA() {
		return a;
	}
	public int getB() {
		return b;
	}
	public int getC() {
		return c;
	}
	
	public double shapeFunc(IntPoint pos) {
		switch (this.getRank()) {
		case 0: return 1.0d / 8.0d * (1 + this.getA() * pos.getXi()) * (1 + this.getB() * pos.getEta()) * (1 + this.getC() * pos.getZeta());
		default:
			assert(false);
			return 0.0d;
		}
	}
	
	public double shapeFuncPartial(int axis, IntPoint pos) {
		switch (this.getRank()) {
		case 0: {
			double ret = 1.0d / 8.0d;
			ret *= (axis == 0) ? this.getA() : (1 + this.getA()*pos.getXi());
			ret *= (axis == 1) ? this.getB() : (1 + this.getB()*pos.getEta());
			ret *= (axis == 2) ? this.getC() : (1 + this.getC()*pos.getZeta());
			return ret;
		}
		default:
			assert(false);
			return 0.0d;
		}
	}
	
}
