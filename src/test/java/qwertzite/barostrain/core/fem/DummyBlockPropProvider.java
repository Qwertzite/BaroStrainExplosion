package qwertzite.barostrain.core.fem;

import net.minecraft.util.math.BlockPos;

public class DummyBlockPropProvider implements IBlockPropertyProvider {
	
	private final double hardness;
	private final double resistance;
	
	public DummyBlockPropProvider(double hardness, double resistance) {
		this.hardness = hardness;
		this.resistance = resistance;
	}
	
	private double getHardness() { return this.hardness; }
	private double getResistance() { return this.resistance; }
	private double getYoungsModulus() {
		double hardness = this.getHardness();
		double resistance = this.getResistance();
		return hardness != 0 ? resistance / hardness : 0.0d;
	}
	
	private double getPoissonCoeff() {
		double hardness = this.getHardness();
		double resistance = this.getResistance();
		return hardness != 0 ? 0.5 * hardness / (hardness + 1.0d) * 3.0d / (resistance / hardness + 2.0d) : 0.0d;
	}

	@Override
	public double getSigmaYield(BlockPos element) {
		return this.getHardness();
	}

	@Override
	public double getMuForElement(BlockPos element) {
		double youngsModulus = this.getYoungsModulus();
		double poissonCoeffs = this.getPoissonCoeff();
		return youngsModulus / (2*(1 + poissonCoeffs));
	}

	@Override
	public double getLambdaForElement(BlockPos element) {
		double youngsModulus = this.getYoungsModulus();
		double poissonCoeffs = this.getPoissonCoeff();
		return youngsModulus * poissonCoeffs / ((1 + poissonCoeffs)*(1 - 2*poissonCoeffs));
	}

}
