package qwertzite.barostrain.core.fem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import qwertzite.barostrain.core.common.coord.BlockFace;
import qwertzite.barostrain.core.common.coord.ElemVertex;
import qwertzite.barostrain.core.common.coord.IntPoint;
import qwertzite.barostrain.core.common.coord.VertexPos;

public class FEM {
	/**
	 * @see VertexPos#fromBlockFace(BlockFace)
	 */
	public static final int MAX_VERTEX_RANK = 0;
	
	private IBlockPropertyProvider ibpp;
	
	private Map<VertexPos, Vec3d> externalForce = new HashMap<>();
	private Map<BlockPos, Vec3d[]> externalForceByElement = new HashMap<>();
	
	public FEM(IBlockPropertyProvider ibpp) {
		this.ibpp = ibpp;
	}
	
	public void applyPressure(BlockFace bf, double force) {
		ElemVertex[] elemVertex = CoordHelper.memberElemVertex(bf.getFacing());
		VertexPos[] vertex = CoordHelper.memberVertexPos(bf);
		Vec3i dir = bf.getFacing().getDirectionVec();
		for (VertexPos pos : vertex) {
			synchronized (externalForce) {
				externalForce.put(pos, externalForce.getOrDefault(pos, Vec3d.ZERO).addVector(-force*dir.getX(), -force*dir.getY(), -force*dir.getZ()));
			}
		}
		Vec3d[] prev;
		synchronized (externalForceByElement) {
			if (!externalForceByElement.containsKey(bf.getBlockpos())) {
				externalForceByElement.put(bf.getBlockpos(), prev = new Vec3d[ElemVertex.values().length]);
			} else {
				prev = externalForceByElement.get(bf.getBlockpos());
			}
		}
		synchronized (prev) {
			for (ElemVertex ev : elemVertex) {
				prev[ev.getIndex()] = prev[ev.getIndex()].addVector(-force*dir.getX(), -force*dir.getY(), -force*dir.getZ());
			}
		}
	}
	
	public Set<BlockPos> femExec() {
		FemIter iteration = new FemIter(this.externalForce);
		
		Set<BlockPos> targetElements = iteration.getForceBalance().entrySet().parallelStream()
				.filter(e -> {
					Vec3d vec = e.getValue();
					double tolerance = getTolerance(e.getKey());
					return Math.abs(vec.x) >= tolerance || Math.abs(vec.y) >= tolerance || Math.abs(vec.z) >= tolerance; })
//				.peek(e -> {}) ????????????????????????????????????
				.flatMap(e -> Stream.of(e.getKey().getBelongingElements()))
				.collect(Collectors.toSet());
		
//		while (!targetElements.isEmpty())
		{
			this.computeVertexForce(iteration, targetElements);
			
			iteration = new FemIter(iteration);
			targetElements = iteration.getForceBalance().entrySet().parallelStream() // ??????????????????????????????????????????????????????????????????
					.filter(e -> {
						Vec3d vec = e.getValue();
						double tolerance = getTolerance(e.getKey());
						return Math.abs(vec.x) >= tolerance || Math.abs(vec.y) >= tolerance || Math.abs(vec.z) >= tolerance; })
					.peek(e -> {}) // COMBAK: ????????????????????????????????????
					.flatMap(e -> Stream.of(e.getKey().getBelongingElements()))
					.collect(Collectors.toSet()); // ?????????????????????????????????????????????
		}
		
		// COMBAK: ????????????
		
		return Collections.emptySet();
	}
	
	private void computeVertexForce(FemIter iteration, Set<BlockPos> targetElements) {
		
		targetElements.parallelStream() // PARALLEL
			.forEach(e -> {// ??????????????????????????????????????????????????????
				iteration.clearTargetElement(e); // ????????????????????????????????????????????????????????????
				
				final double mu = this.ibpp.getMuForElement(e);
				final double lambda = this.ibpp.getLambdaForElement(e);
				final double sigmaYield = this.ibpp.getSigmaYield(e);
				final double mass = this.ibpp.getMass(e);
				
				final int NV = ElemVertex.values().length;
				VertexPos[] absPos = CoordHelper.memberVertexPos(e);
				Vec3d[] u = new Vec3d[NV]; // ????????????
				for (int i = 0; i < NV; i++) u[i] = iteration.getDisplacement(absPos[i]);
				double[][] intForce = new double[3][NV]; // ????????????
				double[][] inertia = new double[3][NV]; // ?????????
				
				for (IntPoint xi : IntPoint.values()) {// ??????????????????????????????????????????????????????fi ???????????????
					double[][] epsilon = new double[3][3]; // ??????
					
					 // ????????????????????????????????????????????????????????????
					for (ElemVertex p : ElemVertex.values()) { // ????????????
						for (int i = 0; i < 3; i++) {
							for (int j = 0; j < 3; j++) {
								epsilon[i][j] += (
										p.shapeFuncPartial(i, xi) * u(u[p.getIndex()], j) +
										p.shapeFuncPartial(j, xi) * u(u[p.getIndex()], i)
										) / 2.0d;
							}
						}
					}
					
					// ??????????????????
					double epsm = 0.0d;
					for (int i = 0; i < 3; i++) epsm += epsilon[i][i];
					epsm /= 3.0d;
					for (int i = 0; i < 3; i++) {
						epsilon[i][i] -= epsm; // ???????????????????????????????????? e
					}
					
					double[][] sigmaTrial = new double[3][3];
					double sigmaNormal = 0.0d;
					for (int i = 0; i < 3; i++) {
						for (int j = 0; j < 3; j++) {
							sigmaTrial[i][j] = 2 * mu * epsilon[i][j];
							sigmaNormal += sigmaTrial[i][j] * sigmaTrial[i][j];
						}
					}
					sigmaNormal = MathHelper.sqrt(sigmaNormal);
					double ftrial = sigmaNormal - MathHelper.sqrt(2.0d / 3.0d) * sigmaYield;
					double slide = ftrial / (2*mu);
					
					double[][] sigma = new double[3][3];
					
					if (slide > 0.0d) { // ??????????????????????????????
						double[][] normal = new double[3][3];
						for (int i = 0; i < 3; i++) {
							for (int j = 0; j < 3; j++) {
								normal[i][j] = sigmaTrial[i][j] / sigmaNormal;
							}
						}
						
						for (int i = 0; i < 3; i++) {
							for (int j = 0; j < 3; j++) {
								sigma[i][j] = sigmaTrial[i][j] - 2*mu*slide*normal[i][j];
							}
							sigma[i][i] += (2.0d * mu + 3.0d * lambda) * epsm;
						}
					} else { // ????????????????????????????????????
						for (int i = 0; i < 3; i++) {
							for (int j = 0; j < 3; j++) {
								sigma[i][j] = sigmaTrial[i][j]; // ????????????
							}
							sigma[i][i] += (2.0d * mu + 3.0d * lambda) * epsm; // ????????????
						}
					}
					
					double[] displacement = new double[3]; // ?????????xi??????????????????
					for (ElemVertex q : ElemVertex.values()) {
						displacement[0] += q.shapeFunc(xi)*u(u[q.getIndex()], 0);
						displacement[1] += q.shapeFunc(xi)*u(u[q.getIndex()], 1);
						displacement[2] += q.shapeFunc(xi)*u(u[q.getIndex()], 2);
					}
					for (ElemVertex p : ElemVertex.values()) {
//						for (int j = 0; j < 3; j++) {
//							intForce[0][p.getIndex()] += -sigma[0][j]*0.5d*p.shapeFuncPartial(j, xi); TODO: enable
//							intForce[1][p.getIndex()] += -sigma[1][j]*0.5d*p.shapeFuncPartial(j, xi);
//							intForce[2][p.getIndex()] += -sigma[2][j]*0.5d*p.shapeFuncPartial(j, xi);
//						}
						inertia[0][p.getIndex()] += mass * p.shapeFunc(xi)*displacement[0];
						inertia[1][p.getIndex()] += mass * p.shapeFunc(xi)*displacement[1];
						inertia[2][p.getIndex()] += mass * p.shapeFunc(xi)*displacement[2];
					}
				}
				iteration.addExternalForce(e, absPos, intForce, inertia); // ?????????????????????
			});
	}
	
	private double u(Vec3d vec, int i) {
		switch (i) {
		case 0: return vec.x;
		case 1: return vec.y;
		case 2: return vec.z;
		default:
			assert(false);
			return 0.0d;
		}
	}
	
	private double getTolerance(VertexPos vertex) {
		return this.ibpp.getTolerance(vertex);
	}
	
	// ==== ====  results ==== ====
	
	/**
	 * Returns the force exerted by the inertia of the block on the given face,
	 * i.e. the transmitted component of in case of block destruction.
	 * In the future, it might be better to make the shock wave to be
	 * transmitted even if the block is not destroyed.
	 * This function is expected to be called only once for each face.
	 */
	public double getInertialForceFor(BlockFace face) { // IMPL:
		return 0.0d;
	}
	
	public void markAsDestoyedBlock(BlockPos pos) {
		// TODO:
	}
	
//	public void notifyBlockStatusChange(Set<BlockPos> destroyedBlocks) {
//		for (BlockPos blockpos : destroyedBlocks) {
//			VertexPos[] poss = VertexPos.fromElementPos(blockpos);
//			for (VertexPos vp : poss) {
//				this.inbalanceTolerance.remove(vp);
//			}
//		}
//	}
	

}
