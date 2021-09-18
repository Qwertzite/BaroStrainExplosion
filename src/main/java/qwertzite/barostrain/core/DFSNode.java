package qwertzite.barostrain.core;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class DFSNode {
	
	private final BlockPos pos;
	private final EnumFacing forceCameFrom;
	private EnumFacing prevFacing;
	private int faceIndex = 0;
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
	
	public EnumFacing getNextFacing() {
		if (prevFacing == null) return prevFacing = EnumFacing.UP; // １番目
		if (prevFacing == EnumFacing.DOWN) return null; // 終わり
		if (faceIndex >= 4) return prevFacing = EnumFacing.DOWN; // 最後

		BlockPos pos = this.getPos();
		int shift = faceIndex + (pos.getY() % 4) + (pos.getX() % 4) + (pos.getZ()%2)*2;
		shift %= 4;
		if (shift < 0) shift += 4;
		int index = ((shift&0b1) ^ ((shift>>1)&0b1)) + (shift&0b10) - faceIndex;
		if (index < 0) index += 4;
		faceIndex++;
		return EnumFacing.getHorizontal(index);
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
