package qwertzite.barostrain.core;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class DFSNode {
	
	private final BlockPos pos;
	private final EnumFacing forceCameFrom;
	private EnumFacing prevFacing;
	private final int depth;
	private double remainingFlowCap;
	
	
	public DFSNode(BlockPos pos, int depth, double flowCap) {
		this(pos, depth, flowCap, null);
	}
	
	/**
	 * 
	 * @param pos
	 * @param depth
	 * @param flowCap
	 * @param forceCameFrom この位置から見てこちらの方向から力が伝わってきた．
	 */
	public DFSNode(BlockPos pos, int depth, double flowCap, EnumFacing forceCameFrom) {
		this.pos = pos;
		this.forceCameFrom = forceCameFrom;
		this.prevFacing = null;
		this.depth = depth;
		this.remainingFlowCap = flowCap;
	}
	
	public EnumFacing getFaceForceCameFrom() {
		return this.forceCameFrom;
	}
	
	public EnumFacing getPrevFacing() {
		return prevFacing;
	}

	public void setPrevFacing(EnumFacing prevFacing) {
		this.prevFacing = prevFacing;
	}
	
	public EnumFacing getNextFacing() {
		int ni;
		if (prevFacing == null) ni = 0;
		else ni = this.prevFacing.getIndex() + 1;
		if (ni >= EnumFacing.VALUES.length) return null;
		return this.prevFacing = EnumFacing.VALUES[ni];
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
