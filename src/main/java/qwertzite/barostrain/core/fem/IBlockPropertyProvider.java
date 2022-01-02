package qwertzite.barostrain.core.fem;

import net.minecraft.util.math.BlockPos;
import qwertzite.barostrain.core.common.coord.VertexPos;

public interface IBlockPropertyProvider {
	public void markAsStateChanged(BlockPos pos);
	public double getTolerance(VertexPos glbVertex);
	public double getSigmaYield(BlockPos element);
	public double getMuForElement(BlockPos element);
	public double getLambdaForElement(BlockPos element);
	public double getMass(BlockPos element);
}
