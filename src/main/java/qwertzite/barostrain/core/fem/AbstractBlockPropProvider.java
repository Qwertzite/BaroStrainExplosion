package qwertzite.barostrain.core.fem;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.util.math.BlockPos;
import qwertzite.barostrain.core.common.coord.VertexPos;

public abstract class AbstractBlockPropProvider implements IBlockPropertyProvider {
	
	private Set<BlockPos> destroyeds = new HashSet<>();
	
	private Object2DoubleMap<VertexPos> tolerance = new Object2DoubleOpenHashMap<>();
	private ReadWriteLock toleranceLock = new ReentrantReadWriteLock();

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
	public double getTolerance(VertexPos glbVertex) {
		if (this.tolerance.containsKey(glbVertex)) return 0.0d; // 破壊判定されている場合
		this.toleranceLock.readLock().lock();
		if (!this.tolerance.containsKey(glbVertex)) {
			this.toleranceLock.readLock().unlock();
			
			this.toleranceLock.writeLock().lock();
			if (!this.tolerance.containsKey(glbVertex)) {
				boolean destroyed = true;
				double resistance = 0.0d;
				for (BlockPos elem : glbVertex.getBelongingElements()) {
					if (!this.isDestoryed(elem)) {
						destroyed = false;
						resistance = Math.max(resistance, this.resistance(elem) / 16);
					}
				}
				if (destroyed) resistance = Double.MAX_VALUE / 16;
				
				this.tolerance.put(glbVertex, resistance);
				this.toleranceLock.writeLock().unlock();
				return resistance;
			}
			this.toleranceLock.writeLock().unlock();
			
			this.toleranceLock.readLock().lock();
		}
		double resistance = this.tolerance.getDouble(glbVertex);
		this.toleranceLock.readLock().unlock();
		return resistance;
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
