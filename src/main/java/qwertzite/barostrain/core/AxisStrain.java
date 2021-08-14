package qwertzite.barostrain.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.math.BlockPos;
import qwertzite.barostrain.core.BSExplosionBase.PressureRay;

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
	private final BlockStrainSimulator simulator;
	private final Axis axis;
	
	private final Map<BlockPos, StrainStatus> strainmap = Collections.synchronizedMap(new HashMap<>());
	
	private final Map<BlockFace, Set<PressureRay>> collidedRays = new HashMap<>();
	private final Object2DoubleMap<BlockFace> initialForce = new Object2DoubleOpenHashMap<>();
	private final Object2DoubleMap<BlockFace> remainingForce = new Object2DoubleOpenHashMap<>();
	
	
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
		double f = this.remainingForce.getDouble(bf) + force;
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
		
		boolean masterFlag = true;
		int maxDepth = 0;
		
		for (BlockFace bf : this.remainingForce.keySet()) {
			BlockPos pos = bf.getBlockpos();
			boolean flag = pos.getZ() == 1 && pos.getY() >= 63 && pos.getX() >= 38 && pos.getX() <= 41;
			if (flag) System.out.println("remainingforce = " + BSExplosionBase.isZero(remainingForce.getDouble(bf)));
		}

		Object2DoubleMap<BlockFace> originalForce = new Object2DoubleOpenHashMap<>(this.remainingForce);
		
		while (masterFlag) { // これ以上探索最大深さを増やしても無駄な時に終了
			masterFlag = false;
			
			Object2DoubleMap<BlockFace> remainingNext = new Object2DoubleOpenHashMap<>();
			for (BlockFace bf : this.remainingForce.keySet()) { // それぞれの圧力がかかった面に対して
				
				// 衝撃波から受けている力の向き
				double forceAppliedByBlast = this.remainingForce.getDouble(bf) * (this.isDirPositive(bf.getFacing().getAxisDirection()) ? -1.0d : 1.0d);
				{
					StrainStatus strstat = this.getStrainStatus(bf.getBlockpos()); // 最初のブロックの状態
					DFSNode node0 = new DFSNode(strstat.getPos(), maxDepth, forceAppliedByBlast);
					dfs.push(node0);
				}
				
				while (!dfs.isEmpty()) { // メインのDFS，力を伝えられる経路を探す
					DFSNode currentNode = dfs.peek();
					BlockPos currentPos = currentNode.getPos();
					StrainStatus currentStrStat = this.getStrainStatus(currentPos);
					
					double flowReachedHere = currentNode.getMaximumFlow(); // 今いる場所まで流せる最大の力
					if (BSExplosionBase.isZero(flowReachedHere)) {
						dfs.pop(); // 自分のところまで流れてくる力の最大値が0だったら中止する．
						continue;
					}
					
					// 既に遠くまで調査済みかを調べる．これはソースに依らない
					Object2IntMap<BlockPos> searchedDepthMap = flowReachedHere > 0 ? pos2depthP : pos2depthN;
					if (currentNode.getDepth() <= searchedDepthMap.getInt(currentPos)) { dfs.pop(); continue; } // 既により遠くまで調査済み
					searchedDepthMap.put(currentPos, currentNode.getDepth());
					
					final double absorvingCap = this.canAbsorb(currentPos, flowReachedHere);
					if (!BSExplosionBase.isZero(absorvingCap)) { // 現在のブロックが力を慣性力で吸収できる場合
						forceAppliedByBlast -= absorvingCap;
						currentStrStat.absorveForce(absorvingCap); // 慣性力での吸収
						for (Iterator<DFSNode> itr = dfs.iterator(); itr.hasNext();) {
							DFSNode n = itr.next();
							double ncap = n.getMaximumFlow() - absorvingCap;
							n.setMaximumFlow(ncap);
							if (BSExplosionBase.isZero(ncap)) { // これによって，流れを受け取れるところまで戻る．
//								searchedDepthMap.removeInt(n.getPos());
								itr.remove();
							}
							if (n.getPrevFacing() != null) { // 自分が吸収したのでない場合
								this.flowForceThroughFace(currentPos, n.getPrevFacing(), absorvingCap);
							}
						}
						// 現在のブロックがさらに吸収できる場合，ncap == 0 になるのでDFSを戻る．
						// そうでなければ，ncap != 0 になるのでそのままになる．
						if (currentStrStat.hasReachedCapacity()) { // 新たに吸収上限に達したブロックは周囲のstatusをチェックする．これにより必ずcap上限に達したブロックはcapに余裕のあるブロックに接する
							for (EnumFacing facing : EnumFacing.values()) { this.getStrainStatus(currentPos.offset(facing)); }
						}
					} else { // 自分では吸収できない場合　次のノードに流せるかを確かめる．
						int depth = currentNode.getDepth();
						if (depth <= 0) {
							masterFlag = true; //継続フラグを立てる　深さによる探索上限に達したため，もう一度深くして探索
							dfs.pop(); // 戻る
							continue;
						} else { // 次のノードに流せる力を計算する．
							dfs.pop(); // 自分を取り除いておく．次に行くときは改めて push
							for (EnumFacing f = currentNode.getNextFacing(); f != null; f = currentNode.getNextFacing()) {
								BlockPos npos = currentPos.offset(f); // 次の場所の候補
								if (dfs.includes(npos)) continue; // 探索中の場所の場合はスキップ
								double maxFlowForFace = this.calcMaxFlowForFace(currentPos, f, flowReachedHere);
//								System.out.println("face " + f + " pos " + currentPos);
								if (BSExplosionBase.isZero(maxFlowForFace)) continue; // この面には力を伝える余力はない
								
								dfs.push(currentNode);
								dfs.push(new DFSNode(npos, depth-1, maxFlowForFace, f.getOpposite())); // この面から伝えられる最大の力
								break;
							}
						}
					}
				}
				if (!BSExplosionBase.isZero(forceAppliedByBlast)) { remainingNext.put(bf, forceAppliedByBlast); } // 伝わった分は取り除いてセットしなおす
				dfs.clear();
				
				pos2depthP.clear(); // FIXME: 今はソースごとにクリアしている
				pos2depthN.clear();
			}
			this.remainingForce.clear();
			this.remainingForce.putAll(remainingNext);
			remainingNext.clear();
			
			maxDepth++; // 次の週はより深くまで探索する
		}
		System.out.println("master itr " + this.axis + " " + maxDepth);
		
		// この段階で残っているすべての BF は吸収しきれなかった分 = 透過率を表している．
		// strain status を調べ，absorbable なものとつながっていない = Min(min - c, max- c) != 0 のブロックは
		// 吹き飛ばされた扱いになる．つながっているかどうかは，自身がnon-absorbable かつabsorbable なものとつながっていない場合．
		// というよりも，つながっているものから辿って取り除いていく．
		
		System.out.println("Axis " + this.axis + " strainresult " + this.strainmap.size());
		
		
		Set<BlockPos> notCheckedYet = new HashSet<>(this.strainmap.keySet());
		Set<BlockPos> possiblyDestroyed = new HashSet<>(this.strainmap.keySet());
		Set<BlockPos> destroyed = new HashSet<>();
		
//		for (BlockFace bf : originalForce.keySet()) { // Rayがhitした面の判定
//			BlockPos pos = bf.getBlockpos();
//			if (destroyed.contains(pos)) continue;
//			double dir = originalForce.getDouble(bf);
//			if (BSExplosionBase.isZero(dir)) continue;
//			dir = dir > 0 ? 1 : -1;
//			if (this.isFragile(dir, pos)) {
//				notCheckedYet.remove(pos);
//				possiblyDestroyed.remove(pos);
//				destroyed.add(pos);
//			}
//		}
		for (BlockFace bf : this.remainingForce.keySet()) {
			BlockPos pos = bf.getBlockpos();
			if (destroyed.contains(pos)) continue;
			if (BSExplosionBase.isZero(this.remainingForce.getDouble(bf))) continue;
			notCheckedYet.remove(pos);
			possiblyDestroyed.remove(pos);
			destroyed.add(pos);
		}
		
		System.out.println("may not be desroyed = " + notCheckedYet.size() + " destroyed = " + destroyed.size());
		
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
	
	private StrainStatus getStrainStatus(BlockPos blockpos) {
		if (this.strainmap.containsKey(blockpos)) {
			return this.strainmap.get(blockpos);
		} else {
			double hardness = this.simulator.getBlockHardnessAt(blockpos);
			double resistance = this.simulator.getBlockResistanceAt(blockpos);
			StrainStatus stat = new StrainStatus(blockpos, this.axis, resistance, hardness);
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
	private double canAbsorb(BlockPos pos, double applied) {
		StrainStatus stat = this.getStrainStatus(pos);
		return stat.getAbsorveable(applied);
	}
	
	/** 指定したブロックから，faceの方向の境界に力を伝える．受け取るがわの面の処理も行ってくれる */
	private void flowForceThroughFace(BlockPos pos, EnumFacing face, double force) {
		StrainStatus target = this.getStrainStatus(pos);
		target.flowForceThroughFace(face, force);
		this.getStrainStatus(pos.offset(face)).flowForceThroughFace(face.getOpposite(), -force);
	}
	
	/** 受け手，送り手，両方の上限に合わせて上限を確認する */
	private double calcMaxFlowForFace(BlockPos pos, EnumFacing face, double flow) {
		flow = this.getStrainStatus(pos).calcFlowableForceForFace(face, flow);
		flow = -this.getStrainStatus(pos.offset(face)).calcFlowableForceForFace(face.getOpposite(), -flow);
		return flow;
	}
	
	private boolean isFaceConnected(BlockPos pos, EnumFacing face) {
		return !this.getStrainStatus(pos).isElastoPasticDeforming(face) &&
				!this.getStrainStatus(pos.offset(face)).isElastoPasticDeforming(face.getOpposite());
	}
	
	private boolean isFragile(double dir, BlockPos pos) {
		StrainStatus strain = this.getStrainStatus(pos);
		
		boolean flag = pos.getZ() == 11 && pos.getY() >= 63 && pos.getX() >= 38 && pos.getX() <= 41;
//		flag = true;
		if (flag) System.out.println("****pos = " + pos + " cap = " + strain.hasReachedCapacity());
		
		if (!strain.hasReachedCapacity()) return false;
		for (EnumFacing facing : EnumFacing.VALUES) {
			if (flag) System.out.println("facing=" + facing + " con=" + this.isFaceConnected(pos, facing) + " trans=" + strain.getTransmittingForce(facing) + " dir=" + dir );
			if (this.isFaceConnected(pos, facing) && strain.getTransmittingForce(facing)*dir > -BSExplosionBase.ERR) return false;
		}
		return true;
	}
	
	public Stream<PressureRay> rayRefAndTr() {
		return this.collidedRays.entrySet().parallelStream().flatMap(e -> {
			BlockFace bf = e.getKey();
			Set<PressureRay> rays = e.getValue(); 
			double initial = this.initialForce.getDouble(bf);
			double remain = this.remainingForce.containsKey(bf) ? this.remainingForce.getDouble(bf) : 0.0f;
			double tr = BSExplosionBase.isZero(initial) ? 0.0d : remain / initial;
			
			return rays.parallelStream().flatMap(r -> r.reflection(tr, initial - remain));
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