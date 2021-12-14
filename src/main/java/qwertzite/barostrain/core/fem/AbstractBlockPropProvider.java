package qwertzite.barostrain.core.fem;

import net.minecraft.util.math.BlockPos;

public abstract class AbstractBlockPropProvider implements IBlockPropertyProvider {

	protected abstract double hardness(BlockPos pos);
	protected abstract double resistance(BlockPos pos);

	protected double getYoungsModulus(BlockPos pos) {
		double hardness = this.hardness(pos);
		double resistance = this.resistance(pos);
		return hardness != 0 ? resistance / hardness : 0.0d;
	}
	
	protected double getPoissonCoeff(BlockPos pos) {
		double hardness = this.hardness(pos);
		double resistance = this.resistance(pos);
		return hardness != 0 ? 0.5 * hardness / (hardness + 1.0d) * 3.0d / (resistance / hardness + 2.0d) : 0.0d;
	}

	@Override
	public double getMuForElement(BlockPos element) {
		double youngsModulus = this.getYoungsModulus(element);
		double poissonCoeffs = this.getPoissonCoeff(element);
		return youngsModulus / (2*(1 + poissonCoeffs));
	}

	@Override
	public double getLambdaForElement(BlockPos element) {
		double youngsModulus = this.getYoungsModulus(element);
		double poissonCoeffs = this.getPoissonCoeff(element);
		return youngsModulus * poissonCoeffs / ((1 + poissonCoeffs)*(1 - 2*poissonCoeffs));
	}

}
