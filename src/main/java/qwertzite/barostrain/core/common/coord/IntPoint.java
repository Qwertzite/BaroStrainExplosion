package qwertzite.barostrain.core.common.coord;

public enum IntPoint {
	IP_NNN(0, -1, -1, -1, 1.0d /8),
	IP_PNN(1,  1, -1, -1, 1.0d /8),
	IP_NPN(2, -1,  1, -1, 1.0d /8),
	IP_PPN(3,  1,  1, -1, 1.0d /8),
	IP_NNP(4, -1, -1,  1, 1.0d /8),
	IP_PNP(5,  1, -1,  1, 1.0d /8),
	IP_NPP(6, -1,  1,  1, 1.0d /8),
	IP_PPP(7,  1,  1,  1, 1.0d /8),
	;
	
	private final int index;
	private final double x;
	private final double y;
	private final double z;
	private final double weight;
	
	private IntPoint(int index, double x, double y, double z, double weight) {
		this.index = index;
		double D = 1.0d / Math.sqrt(3.0d);
		this.x = x*D;
		this.y = y*D;
		this.z = z*D;
		this.weight = weight;
	}
	
	public int getIndex() { return this.index; }
	public double getXi() { return x; }
	public double getEta() { return y; }
	public double getZeta() { return z; }
	public double getWeight() { return weight; }
}
