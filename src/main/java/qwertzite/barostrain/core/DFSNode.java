package qwertzite.barostrain.core;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class DFSNode {
	
	private final BlockPos pos;
	private EnumFacing prevFacing;
	private final int depth;
	private double remainingFlowCap;
	
	public DFSNode(BlockPos pos, int depth, double flowCap) {
		this.pos = pos;
		this.prevFacing = null;
		this.depth = depth;
		this.remainingFlowCap = flowCap;
	}

	public EnumFacing getPrevFacing() {
		return prevFacing;
	}

	public void setPrevFacing(EnumFacing prevFacing) {
		this.prevFacing = prevFacing;
	}
	
	public EnumFacing getNextFacing() {
		if (prevFacing == null) return EnumFacing.VALUES[0];
		int ni = this.prevFacing.getIndex() + 1;
		if (ni >= EnumFacing.VALUES.length) return null;
		return this.prevFacing = EnumFacing.getFront(ni);
	}

	/**
	 * ここに至るまでの経路によって制限を受けた，ソースからここまで流れてくることのできる力の最大値
	 * @return
	 */
	public double getMaximumFlow() {
		return remainingFlowCap;
	}

	public void setMaximumFlow(double forceCapacity) {
		this.remainingFlowCap = forceCapacity;
	}
	
	public BlockPos getPos() {
		return pos;
	}

	public int getDepth() {
		return depth;
	}
	
	
}
