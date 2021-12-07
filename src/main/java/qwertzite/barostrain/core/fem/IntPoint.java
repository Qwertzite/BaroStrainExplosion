package qwertzite.barostrain.core.fem;

public enum IntPoint {
	IP_NNN(-1, -1, -1, 1.0d /8),
	IP_PNN( 1, -1, -1, 1.0d /8),
	IP_NPN(-1,  1, -1, 1.0d /8),
	IP_PPN( 1,  1, -1, 1.0d /8),
	IP_NNP(-1, -1,  1, 1.0d /8),
	IP_PNP( 1, -1,  1, 1.0d /8),
	IP_NPP(-1,  1,  1, 1.0d /8),
	IP_PPP( 1,  1,  1, 1.0d /8),
	;
	
	
	private final double x;
	private final double y;
	private final double z;
	private final double weight;
	
	private IntPoint(double x, double y, double z, double weight) {
		double D = 1.0d / Math.sqrt(3.0d);
		this.x = x*D;
		this.y = y*D;
		this.z = z*D;
		this.weight = weight;
	}

	public double getXi() {
		return x;
	}

	public double getEta() {
		return y;
	}

	public double getZeta() {
		return z;
	}

	public double getWeight() {
		return weight;
	}
	
}
