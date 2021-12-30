package qwertzite.barostrain.core.fem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import qwertzite.barostrain.core.common.coord.ElemVertex;
import qwertzite.barostrain.core.common.coord.IntPoint;
import qwertzite.barostrain.core.common.coord.VertexPos;

public class FemIter { // FIXME: parallel execution.
	/** Add 2 Vec3d objects and returns new object. */
	private static final BiFunction<Vec3d, Vec3d, Vec3d> MERGER_V3 = (v1, v2) -> v1.add(v2);
	private static final BiFunction<Vec3d[], Vec3d[], Vec3d[]> MERGER_V3A = (v1, v2) -> {
		Vec3d[] v3 = new Vec3d[v1.length];
		for (int i = 0; i < v1.length; i++) v3[i] = v1[i].add(v2[i]);
		return v3;
	};
	
	// ========  preconditions  ========
	private Map<VertexPos, Vec3d> prevDisplacement;
	private Map<VertexPos, Vec3d> displacement;
	
	// ========  results  ========
	private Map<VertexPos, Vec3d> forceBalance;
	/**
	 * direction is inverted<br>
	 * force vectors are sorted by ElemVertex index.
	 */
	private Map<BlockPos, Vec3d[]> vertexForcePerElem;
	private Object2IntMap<BlockPos> elasticDeformation;
	/** force array is sorted by EnumFacing index. Positive direction is equal to that of each axis. */
	private Map<BlockPos, double[]> inertialForceForFace;
	
	public FemIter(Map<VertexPos, Vec3d> externalForce) {
		this.prevDisplacement = Collections.emptyMap();
		this.displacement = Collections.emptyMap();
		this.vertexForcePerElem = new HashMap<>();
		this.forceBalance = new HashMap<>(externalForce);
		this.elasticDeformation = new Object2IntOpenHashMap<>();
	}
	
	public FemIter(FemIter prev) {
		this.prevDisplacement = prev.displacement;
		this.displacement = new HashMap<>(prev.displacement);
		this.vertexForcePerElem = new HashMap<>(prev.vertexForcePerElem);
		this.forceBalance = new HashMap<>(prev.forceBalance);
		this.elasticDeformation = new Object2IntOpenHashMap<>(prev.elasticDeformation);
	}
	
	// ========  preconditions  ========
	
	/**
	 * Subtract the effect of the internal forces exerted by the given element.
	 * @param target
	 */
	public void clearTargetElement(BlockPos target) { // FIXME: parallel invocation
		Vec3d[] elemForce = this.vertexForcePerElem.remove(target);
		
		if (elemForce != null) {
			for (ElemVertex ev : ElemVertex.values()) {
				Vec3d ef = elemForce[ev.getIndex()];
				if (ef != null) {
					this.forceBalance.merge(new VertexPos(target, ev), ef, MERGER_V3);
				}
			}
		}
		this.elasticDeformation.removeInt(target);
	}
	
	/**
	 * Note that this field is initialised with ZERO.
	 * @param displacement
	 */
	public void setDisplacement(Map<VertexPos, Vec3d> displacement) { // TODO: 平行化を考慮して，実装を変えるかも
		this.displacement.putAll(displacement);
	}
	
	// ======== computation data ========
	
	public Map<VertexPos, Vec3d> getForceBalance() { return this.forceBalance; }
	public Vec3d getDisplacement(VertexPos pos) {
		return this.displacement.getOrDefault(pos, Vec3d.ZERO);
	}
	
	// ======== results ========
	
	public void addExternalForce(BlockPos elem, VertexPos[] pos, double[] f0, double[] f1, double[] f2) {
//		VertexPos pos = new VertexPos(element, vertex);
		synchronized (forceBalance) { // OPTIMIZE: use efficient way.
			for (int i = 0; i < pos.length; i++) {
				forceBalance.merge(pos[i], new Vec3d(f0[i], f1[i], f2[i]), MERGER_V3);
			}
		}
		Vec3d[] vs = new Vec3d[pos.length];
		for (ElemVertex ev : ElemVertex.values()) {
			int i = ev.getIndex();
			vs[i] = new Vec3d(-f0[i], -f1[i], -f2[i]); // NOTE: values are inverted so that it can be easily subtracted.
		}
		this.vertexForcePerElem.merge(elem, vs, MERGER_V3A);
	}
	
//	private static final IntBinaryOperator INT_MERGER = (v1, v2) -> v1 | v2;
	private static final BiFunction<Integer, Integer, Integer> INT_MERGER = (v1, v2) -> v1 | v2;
	public void setElasticDeforming(BlockPos pos, IntPoint intPt) { // OPTIMIZE:
		synchronized (elasticDeformation) {
			this.elasticDeformation.merge(pos, 1 << intPt.getIndex(), INT_MERGER);
		}
	}
}
