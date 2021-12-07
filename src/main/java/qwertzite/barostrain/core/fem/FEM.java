package qwertzite.barostrain.core.fem;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import scala.actors.threadpool.Arrays;

public class FEM {
	
	private IBlockPropertyProvider ibpp;
	
	public FEM(IBlockPropertyProvider ibpp) {
		this.ibpp = ibpp;
	}
	
	public void computeVertexForce(FemIter iteration) {
		
		iteration.targetElements.parallelStream() // PARALLEL
			.forEach(e -> {// 各要素について計算し節点外力を求める
				
				final double mu = this.ibpp.getMuForElement(e);
				final double lambda = this.ibpp.getLambdaForElement(e);
				final double sigmaYield = this.ibpp.getSigmaYield(e);
				
				final int NV = ElemVertex.values().length;
				VertexPos[] absPos = VertexPos.fromElementPos(e);
				Vec3d[] u = new Vec3d[NV]; // 節点変位
				for (int i = 0; i < NV; i++) u[i] = iteration.getDisplacement(absPos[i]);
				double[] f0 = new double[NV]; // 節点外力 x1成分
				double[] f1 = new double[NV]; // 節点外力 x2成分
				double[] f2 = new double[NV]; // 節点外力 x3成分
				
				for (IntPoint xi : IntPoint.values()) {// 積分点ごとに生み出す節点外力を計算しfi に加算する
					double[][] epsilon = new double[3][3]; // 変位
					
					 // 各接点の歪とトライアル弾性応力を計算する
					for (ElemVertex p : ElemVertex.values()) { // 節点座標
						for (int i = 0; i < 3; i++) {
							for (int j = 0; j < 3; j++) {
								epsilon[i][j] += (
										p.shapeFuncPartial(i, xi) * u(u[p.getIndex()], j) +
										p.shapeFuncPartial(j, xi) * u(u[p.getIndex()], i)
										) / 2.0d;
							}
						}
					}
					
					// 等軸圧縮成分
					double epsm = 0.0d;
					for (int i = 0; i < 3; i++) epsm += epsilon[i][i];
					epsm /= 3.0d;
					for (int i = 0; i < 3; i++) {
						epsilon[i][i] -= epsm; // ここで偏差成分のみになる e
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
					
					if (slide > 0.0d) { // 塑性変形している場合
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
					} else { // 塑性変形が起こらない場合
						for (int i = 0; i < 3; i++) {
							for (int j = 0; j < 3; j++) {
								sigma[i][j] = sigmaTrial[i][j]; // 偏差成分
							}
							sigma[i][i] += (2.0d * mu + 3.0d * lambda) * epsm; // 等方成分
						}
					}
					
					for (ElemVertex p : ElemVertex.values()) {
						for (int j = 0; j < 3; j++) {
							f0[p.getIndex()] += sigma[0][j]*0.5d*p.shapeFuncPartial(j, xi); // TODO: 慣性成分を加算する
							f1[p.getIndex()] += sigma[1][j]*0.5d*p.shapeFuncPartial(j, xi);
							f2[p.getIndex()] += sigma[2][j]*0.5d*p.shapeFuncPartial(j, xi);
						}
					}
				}
				for (int i = 0; i < NV; i++) { iteration.addExternalForce(absPos[i], f0[i], f1[i], f2[i]); } // 節点外力を集計
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
	
//	private double hardness(BlockPos elem) {
////		if (this.affectedBlocks.containsKey(pos)) return 0.0d; // 破壊判定されている場合
////		if (!this.resistanceMap.containsKey(pos)) {
////			IBlockState iblockstate = this.world.getBlockState(pos);
////			double resistance = this.exploder != null ?
////					this.exploder.getExplosionResistance(this.explosion, this.world, pos, iblockstate)
////					: iblockstate.getBlock().getExplosionResistance(this.world, pos, (Entity) null, this.explosion);
////			this.resistanceMap.put(pos, resistance);
////			return resistance;
////		} else{
////			return this.resistanceMap.getDouble(pos);
////		} TODO:
//		return 0.0d;
//	}
//	
//	private double resistance(BlockPos elem) {
////		if (this.affectedBlocks.containsKey(pos)) return 0.0d;
////		else return this.world.getBlockState(pos).getBlockHardness(this.world, pos); TODO
//		return 0.0d;
//	}
}
