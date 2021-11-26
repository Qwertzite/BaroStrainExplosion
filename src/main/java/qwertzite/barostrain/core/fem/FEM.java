package qwertzite.barostrain.core.fem;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class FEM {
	
	private Map<VertexPos, Vec3d> vertexDisplacement = new HashMap<>();
	private Set<BlockPos> registeredElements = new HashSet<>();
	private Map<VertexPos, Vec3d> vertexForce = new HashMap<>();
	/*
	 * 2. 変位から節点外力との釣り合いを求める
	 *  2.1 要素ごとに節点外力を計算し加算する
	 * 3. 節点外力から次の変位を設定する
	 */
	
	private void step() {
		
		
		

	}
	
	private Map<VertexPos, Vec3d> computeNodeForceForDisplacement(Map<VertexPos, Vec3d> displacement) {
		
		Map<VertexPos, Vec3d> externalForce = new HashMap<>();
		
		this.registeredElements.parallelStream() // PARALLEL
			.forEach(e -> {// 各要素について計算し節点外力を求める
			final int NV = ElementVertex.values().length;
			VertexPos[] absPos = VertexPos.fromElementPos(e);
			double[] f1 = new double[NV]; // 節点外力 x1成分
			double[] f2 = new double[NV]; // 節点外力 x2成分
			double[] f3 = new double[NV]; // 節点外力 x3成分
			
			for (IntPoint ip : IntPoint.values()) {// 積分点ごとに生み出す節点外力を計算しfi に加算する
				
				
				
				// TODO:
				// 節点歪を計算する
				// トライアル弾性応力を計算する
				
			}
			
			for (int i = 0; i < NV; i++) { add(externalForce, absPos[i], f1[i], f2[i], f3[i]); } // 節点外力を集計
		});
		return null; // TODO: 節点外力，応力，塑性歪を返す
	}
	
	
	
	private synchronized void add(Map<VertexPos, Vec3d> map, VertexPos pos, double f1, double f2, double f3) {
		map.put(pos, map.getOrDefault(pos, Vec3d.ZERO).addVector(f1, f2, f3));
	}
	
	private double u(int i, VertexPos p) {
		Vec3d v = vertexDisplacement.getOrDefault(p, Vec3d.ZERO);
		switch (i) {
		case 0: return v.x;
		case 1: return v.y;
		case 2: return v.z;
		default: assert (false); return 0;
		}
	}
	
	/**
	 * 形状関数
	 * @param xi
	 * @param element
	 * @param p
	 * @return
	 */
	private double N(IntPoint xi, ElementVertex p) {
		return 0; // TODO: 
	}
}
