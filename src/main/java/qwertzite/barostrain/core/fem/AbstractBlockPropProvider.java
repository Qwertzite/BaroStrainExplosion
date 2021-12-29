package qwertzite.barostrain.core.fem;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.util.math.BlockPos;
import qwertzite.barostrain.core.common.coord.VertexPos;

public abstract class AbstractBlockPropProvider implements IBlockPropertyProvider {
	
	private Set<BlockPos> destroyeds = new HashSet<>();

	protected boolean isDestoryed(BlockPos pos) {
		return this.destroyeds.contains(pos);
	}
	
	protected abstract double hardness(BlockPos pos);
	protected abstract double resistance(BlockPos pos);
	@Override
	public void markAsStateChanged(BlockPos pos) {
		this.destroyeds.add(pos);
	}

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
	public double getTolerance(VertexPos glbVertex) { // OPTIMIZE: cache?
		boolean destroyed = true;
		double resistance = 0.0d;
		for (BlockPos elem : glbVertex.getBelongingElements()) {
			if (!this.isDestoryed(elem)) {
				destroyed = false;
				resistance = Math.max(resistance, this.resistance(elem) / 16);
			}
		}
		if (destroyed) resistance = Double.MAX_VALUE / 16;
		return resistance;
	}

	@Override
	public double getMuForElement(BlockPos element) { // OPTIMIZE: cache?
		double youngsModulus = this.getYoungsModulus(element);
		double poissonCoeffs = this.getPoissonCoeff(element);
		return youngsModulus / (2*(1 + poissonCoeffs));
	}

	@Override
	public double getLambdaForElement(BlockPos element) { // OPTIMIZE: cache?
		double youngsModulus = this.getYoungsModulus(element);
		double poissonCoeffs = this.getPoissonCoeff(element);
		return youngsModulus * poissonCoeffs / ((1 + poissonCoeffs)*(1 - 2*poissonCoeffs));
	}
	
	@Override
	public double getSigmaYield(BlockPos element) { return this.hardness(element); }
}
