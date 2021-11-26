package qwertzite.barostrain.core.fem;

public enum ElementVertex {
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
	private ElementVertex(int index, int rank, int a, int b, int c) {
		this.index = index;
		this.rank = rank;
		this.a = a;
		this.b = b;
		this.c = c;
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
	
}
