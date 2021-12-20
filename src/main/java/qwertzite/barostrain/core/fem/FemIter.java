package qwertzite.barostrain.core.fem;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import qwertzite.barostrain.core.common.coord.GlbIntPt;
import qwertzite.barostrain.core.common.coord.IntPoint;
import qwertzite.barostrain.core.common.coord.VertexPos;

public class FemIter {
	
	public Set<BlockPos> targetElements;
	public Map<VertexPos, Vec3d> displacement;
	
	public Map<VertexPos, Vec3d> vertexForce = new HashMap<>();
	public Object2BooleanMap<GlbIntPt> elasticDeformation = new Object2BooleanOpenHashMap<>();
	
	public void setDisplacement(Map<VertexPos, Vec3d> displacement) {
		this.displacement = displacement;
	}
	
	public Vec3d getDisplacement(VertexPos pos) {
		return this.displacement.getOrDefault(pos, Vec3d.ZERO);
	}
	
	public void addExternalForce(VertexPos pos, double f0, double f1, double f2) {
//		VertexPos pos = new VertexPos(element, vertex);
		synchronized (vertexForce) {
			vertexForce.put(pos, vertexForce.getOrDefault(pos, Vec3d.ZERO).addVector(f0, f1, f2));
		}
	}
	
	public void setElasticDeforming(BlockPos pos, IntPoint intPt, boolean elastic) {
		GlbIntPt glb = new GlbIntPt(pos, intPt);
		synchronized (elasticDeformation) {
			this.elasticDeformation.put(glb, elastic);
		}
	}
}
