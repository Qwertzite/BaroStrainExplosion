package qwertzite.barostrain.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.math.BlockPos;
import qwertzite.barostrain.core.common.coord.BlockFace;

/**
 * 各ブロックの
 * 		外部からかけられた力 (直接レイにさらされたもののみ)
 * 		釣り合っていない力
 * 		慣性力
 * 		面に掛かっている力
 * 
 * 探索
 * 		そのブロックまでの距離
 * 		そこまで流せる最大量
 * 		前のブロック
 * @author qwertzite
 * @date 2021/04/01
 */
public class AxisStrain {
	public static final long BASE = 100000;
	
	private final BlockStrainSimulator simulator;
	private final Axis axis;
	
	private final Map<BlockPos, BlockStrain> strainmap = Collections.synchronizedMap(new HashMap<>());
	
	private final Map<BlockFace, Set<PressureRay>> collidedRays = new HashMap<>();
	private final Object2LongMap<BlockFace> initialForce = new Object2LongOpenHashMap<>();
	private final Object2LongMap<BlockFace> remainingForce = new Object2LongOpenHashMap<>();
	
	
	public AxisStrain(BlockStrainSimulator simulator, Axis axis) {
		this.simulator = simulator;
		this.axis = axis;
	}
	
	public synchronized void applyForce(PressureRay ray, BlockPos pos, EnumFacing face, double force) {
		BlockFace bf = new BlockFace(pos, face);
		Set<PressureRay> rays;
		if (!this.collidedRays.containsKey(bf)) this.collidedRays.put(bf, rays = new HashSet<>());
		else rays = this.collidedRays.get(bf);
		rays.add(ray);
		force *= this.isDirPositive(face.getAxisDirection()) ? -1 : 1;
		long f = this.remainingForce.getLong(bf) + Math.round(force * BASE);
		this.initialForce.put(bf, f);
		this.remainingForce.put(bf, f);
	}
	
	/**
	 * 
	 * @return destroyed blocks and its speed.
	 */
	public Set<BlockPos> evaluate() {
		DFSStack dfs = new DFSStack();
		Object2IntMap<BlockPos> pos2depthP = new Object2IntOpenHashMap<>();
		Object2IntMap<BlockPos> pos2depthN = new Object2IntOpenHashMap<>();
		pos2depthP.defaultReturnValue(-1);
		pos2depthN.defaultReturnValue(-1);
		
		int maxDepth = 0;
		int minDepth = 0;

//		Object2DoubleMap<BlockFace> originalForce = new Object2DoubleOpenHashMap<>(this.remainingForce);
		
		boolean masterFlag = true;
		boolean reversed = true;
		while (masterFlag || reversed) { // これ以上探索最大深さを増やしても無駄な時に終了
			masterFlag = false;
			reversed = false;
			Object2LongMap<BlockFace> remainingNext = new Object2LongOpenHashMap<>();
			for (BlockFace bf : this.remainingForce.keySet()) { // それぞれの圧力がかかった面に対して
				
				// ブロックが衝撃波から受けている力
				long forceAppliedByBlast = this.remainingForce.getLong(bf);
				{
					
					long forceCap = -this.getStrainStatus(bf.getBlockpos()).calcFlowableInitialForceForFace(bf.getFacing(), -forceAppliedByBlast);
					if (BSExplosionBase.isZero(forceCap)) {
						if (!BSExplosionBase.isZero(forceAppliedByBlast)) { remainingNext.put(bf, forceAppliedByBlast); }
						continue;
					}
					
					BlockStrain strstat = this.getStrainStatus(bf.getBlockpos()); // 最初のブロックの状態
					DFSNode node0 = new DFSNode(strstat.getPos(), maxDepth, forceCap);
					dfs.push(node0);
				}
//				System.out.println("bf " + bf.getBlockpos() + " " + bf.getFacing() + " " + forceAppliedByBlast);
				
				while (!dfs.isEmpty()) { // メインのDFS，力を伝えられる経路を探す
					DFSNode currentNode = dfs.peek();
					BlockPos currentPos = currentNode.getPos();
					BlockStrain currentStrStat = this.getStrainStatus(currentPos);
					
					long flowReachedHere = currentNode.getMaximumFlow(); // 今いる場所まで流せる最大の力
					if (flowReachedHere == 0) { // if (BSExplosionBase.isZero(flowReachedHere)) {
						dfs.pop(); // 自分のところまで流れてくる力の最大値が0だったら中止する．
						continue;
					}
					
					// 既に遠くまで調査済みかを調べる．これはソースに依らない
					Object2IntMap<BlockPos> searchedDepthMap = flowReachedHere > 0 ? pos2depthP : pos2depthN;
					searchedDepthMap.put(currentPos, currentNode.getDepth());
					
					final long absorvingCap = this.canAbsorb(currentPos, flowReachedHere);
					if (!BSExplosionBase.isZero(absorvingCap)) { // 現在のブロックが力を慣性力で吸収できる場合
//						System.out.println("absorved " + currentPos + " " + absorvingCap);
						reversed |= flowReachedHere * currentStrStat.getAbsorved() < 0; // 反転時継続
						forceAppliedByBlast -= absorvingCap;
						currentStrStat.absorveForce(absorvingCap); // 慣性力での吸収
						for (Iterator<DFSNode> itr = dfs.iterator(); itr.hasNext();) {
							DFSNode n = itr.next();
							long ncap = n.getMaximumFlow() - absorvingCap;
							n.setMaximumFlow(ncap);
							if (BSExplosionBase.isZero(ncap)) { // これによって，流れを受け取れるところまで戻る．
								searchedDepthMap.removeInt(n.getPos());
								itr.remove();
							}
							if (n != currentNode) { // 自分が吸収したのでない場合
								this.flowForceThroughFace(n.getPos(), n.getPrevFacing(), absorvingCap);
							}
						}
						// 圧力が加わった面の計算
						this.getStrainStatus(bf.getBlockpos()).flowForceThroughFace(bf.getFacing(), -absorvingCap);
						
						// 現在のブロックがさらに吸収できる場合，ncap == 0 になるのでDFSを戻る．
						// そうでなければ，ncap != 0 になるのでそのままになる．
						if (currentStrStat.hasReachedCapacity()) { // 新たに吸収上限に達したブロックは周囲のstatusをチェックする．これにより必ずcap上限に達したブロックはcapに余裕のあるブロックに接する
							for (EnumFacing facing : EnumFacing.values()) { this.getStrainStatus(currentPos.offset(facing)); }
						}
					} else { // 自分では吸収できない場合　次のノードに流せるかを確かめる．
						int depth = currentNode.getDepth();
						if (depth <= minDepth) {
							masterFlag = true; //継続フラグを立てる　深さによる探索上限に達したため，もう一度深くして探索
							dfs.pop(); // 戻る
							continue;
						} else { // 次のノードに流せる力を計算する．
							dfs.pop(); // 自分を取り除いておく．次に行くときは改めて push
							for (EnumFacing f = currentNode.getNextFacing(); f != null; f = currentNode.getNextFacing()) {
								BlockPos npos = currentPos.offset(f); // 次の場所の候補
//								System.out.println(currentPos + " " + f + " " + npos);
								if (dfs.includes(npos)) continue; // 探索中の場所の場合はスキップ
								if (depth-1 <= searchedDepthMap.getInt(npos)) continue; // TO DO: 逆流する場合は許容する
								long maxFlowForFace = this.calcMaxFlowForFace(currentPos, f, flowReachedHere);
//								System.out.println("face " + f + " pos " + currentPos);
								if (BSExplosionBase.isZero(maxFlowForFace)) continue; // この面には力を伝える余力はない
//								System.out.println("next " + npos + " " + maxFlowForFace);
								dfs.push(currentNode);
								dfs.push(new DFSNode(npos, depth-1, maxFlowForFace, f.getOpposite())); // この面から伝えられる最大の力
								break;
							}
						}
					}
//					System.out.println("dfs_size=" + dfs.size());
//					for (BlockStrain bs : this.strainmap.values()) {
//						if (bs.getBlastResistance() != 0.0d) System.out.println(bs);
//					}
				}
				if (!BSExplosionBase.isZero(forceAppliedByBlast)) { remainingNext.put(bf, forceAppliedByBlast); } // 伝わった分は取り除いてセットしなおす
				dfs.clear();
				
				pos2depthP.clear(); // FIXME: 今はソースごとにクリアしている
				pos2depthN.clear();
			}
			
			this.remainingForce.clear();
			this.remainingForce.putAll(remainingNext);
			remainingNext.clear();
			
//			maxDepth++;
//			if (masterFlag) maxDepth++;
			maxDepth++; // 次の週はより深くまで探索する
			if (reversed) minDepth++; // 打消しが発生した場合遠くへは行かない
		}
		
		// この段階で残っているすべての BF は吸収しきれなかった分 = 透過率を表している．
		// strain status を調べ，absorbable なものとつながっていない = Min(min - c, max- c) != 0 のブロックは
		// 吹き飛ばされた扱いになる．つながっているかどうかは，自身がnon-absorbable かつabsorbable なものとつながっていない場合．
		// というよりも，つながっているものから辿って取り除いていく．
		
		Set<BlockPos> notCheckedYet = new HashSet<>(this.strainmap.keySet());
		Set<BlockPos> possiblyDestroyed = new HashSet<>(this.strainmap.keySet());
		Set<BlockPos> destroyed = new HashSet<>();
		
//		for (Map.Entry<BlockPos, BlockStrain> e : this.strainmap.entrySet()) {
//			if (e.getValue().getBlastResistance() != 0) System.out.println(e.getValue());
//		}
		
		for (BlockFace bf : this.remainingForce.keySet()) {
			BlockPos pos = bf.getBlockpos();
			if (destroyed.contains(pos)) continue;
			if (this.remainingForce.getLong(bf) == 0) continue;
//			if (BSExplosionBase.isZero(this.remainingForce.getLong(bf))) continue;
			notCheckedYet.remove(pos);
			possiblyDestroyed.remove(pos);
			destroyed.add(pos);
		}
		
//		System.out.println(this.axis + " remaining=" + this.remainingForce.size() + " may not be desroyed = " + notCheckedYet.size() + " destroyed = " + destroyed.size());
		
		Iterator<BlockPos> itr = notCheckedYet.iterator();
		while (!notCheckedYet.isEmpty()) {
			BlockPos pos0 = itr.next();
			itr.remove();
			
			if (!this.getStrainStatus(pos0).hasReachedCapacity()) {
				possiblyDestroyed.remove(pos0);
//				notCheckedYet.remove(pos0); already removed by itr.remove()
				
				LinkedList<BlockPos> queue = new LinkedList<>();
				queue.add(pos0);
				
				while (!queue.isEmpty()) {
					BlockPos pos = queue.pop();
					for (EnumFacing face : EnumFacing.values()) {
						BlockPos npos = pos.offset(face);
						if (possiblyDestroyed.contains(npos) && this.isFaceConnected(pos, face)) {
							possiblyDestroyed.remove(npos);
							notCheckedYet.remove(npos);
							queue.add(npos);
						}
					}
				}
				itr = notCheckedYet.iterator();
			}
		}
		destroyed.addAll(possiblyDestroyed);
		
		return destroyed;
	}
	
	private BlockStrain getStrainStatus(BlockPos blockpos) {
		if (this.strainmap.containsKey(blockpos)) {
			return this.strainmap.get(blockpos);
		} else {
			double hardness = this.simulator.getBlockHardnessAt(blockpos);
			double resistance = this.simulator.getBlockResistanceAt(blockpos);
			BlockStrain stat = new BlockStrain(blockpos, this.axis, resistance, hardness);
			this.strainmap.put(blockpos, stat);
			return stat;
		}
	}
	
	private boolean isDirPositive(AxisDirection dir) {
		switch (dir) {
		case NEGATIVE: return false;
		case POSITIVE: return true;
		default: assert(false);
		return false;
		}
	}
	
	/**
	 * pos で表される位置のブロックに対し，appliedの力が掛かった時に，慣性力で吸収できる分の最大値
	 * @param pos
	 * @param applied
	 * @return
	 */
	private long canAbsorb(BlockPos pos, long applied) {
		BlockStrain stat = this.getStrainStatus(pos);
		return stat.getAbsorveable(applied);
	}
	
	/** 指定したブロックから，faceの方向の境界に力を伝える．受け取るがわの面の処理も行ってくれる */
	private void flowForceThroughFace(BlockPos pos, EnumFacing face, long force) {
		this.getStrainStatus(pos)             .flowForceThroughFace(face, force);
		this.getStrainStatus(pos.offset(face)).flowForceThroughFace(face.getOpposite(), -force);
	}
	
	/** 受け手，送り手，両方の上限に合わせて上限を確認する */
	private long calcMaxFlowForFace(BlockPos pos, EnumFacing face, long flow) {
		long ret;
		ret = this.getStrainStatus(pos).calcFlowableForceForFace(face, flow);
		ret = -this.getStrainStatus(pos.offset(face)).calcFlowableForceForFace(face.getOpposite(), -ret);
		return ret*flow < 0 ? 0 : flow;
	}
	
	private boolean isFaceConnected(BlockPos pos, EnumFacing face) {
		return !this.getStrainStatus(pos).isElastoPasticDeforming(face) &&
				!this.getStrainStatus(pos.offset(face)).isElastoPasticDeforming(face.getOpposite());
	}
	
	public Stream<PressureRay> rayRefAndTr() {
		return this.collidedRays.entrySet().parallelStream().flatMap(e -> {
			BlockFace bf = e.getKey();
			Set<PressureRay> rays = e.getValue(); 
			double initial = this.initialForce.getLong(bf);
			double remain = this.remainingForce.containsKey(bf) ? this.remainingForce.getLong(bf) : 0.0f;
			double tr = BSExplosionBase.isZero(initial) ? 0.0d : remain / initial;
			
			return rays.parallelStream().flatMap(r -> r.reflection(tr));
		});
	}
	
	/**
	 * Remove blocks which were evaluated to be destroyed by other axes.
	 * @param poss
	 */
	public void checkBlocksDestroyed(Set<BlockPos> poss) {
		for (BlockPos pos: poss) this.strainmap.remove(pos);
	}
	
	public void clear() {
		this.collidedRays.clear();
		this.initialForce.clear();
		this.remainingForce.clear();
	}
	
	public Set<BlockPos> getWiggledBlocks() {
		return this.strainmap.keySet();
	}
	
	// BFSを行い空き容量のある所に流す
	// 破壊されたと判定されるブロックを集計する
	
	private static class DFSStack {
		private LinkedList<DFSNode> stack = new LinkedList<>();
		private Set<BlockPos> posset = new HashSet<>();

		/**
		 * Inserts the element at the front of this list.
		 * @param node
		 */
		public void push(DFSNode node) {
			this.stack.push(node);
			this.posset.add(node.getPos());
		}
		
		public boolean isEmpty() {
			return this.stack.isEmpty();
		}
		
		@SuppressWarnings("unused")
		public int size() {
			return this.posset.size();
		}
		
		public boolean includes(BlockPos pos) {
			return this.posset.contains(pos);
		}
		
		/** Retrieves, but does not remove, the head (first element) of this list. */
		public DFSNode peek() { return this.stack.peek(); }
		
		public Iterator<DFSNode> iterator() { return this.stack.iterator(); }
		
		/**
		 * Pops an element from the stack represented by this list.
		 * In other words, removes and returns the first element of this list.
		 * This method is equivalent to removeFirst().
		 * @return
		 */
		public DFSNode pop() {
			DFSNode node = this.stack.pop();
			this.posset.remove(node.getPos());
			return node;
		}
		
		public void clear() {
			this.posset.clear();
			this.stack.clear();
		}
	}
}
