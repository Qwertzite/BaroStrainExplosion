package qwertzite.barostrain.core.fem;

import net.minecraft.util.math.BlockPos;

public interface IBlockPropertyProvider {
	public double getSigmaYield(BlockPos element);
	public double getMuForElement(BlockPos element);
	public double getLambdaForElement(BlockPos element);
}
