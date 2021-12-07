package qwertzite.barostrain.core.fem;

import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class FemIter {
	
	public Set<BlockPos> targetElements;
	public Map<VertexPos, Vec3d> displacement;
	
	public Map<VertexPos, Vec3d> vertexExternalforce;
	public Object2BooleanMap<GlbIntPt> elasticDeformation;
	
	public Vec3d getDisplacement(VertexPos pos) {
		return this.displacement.get(pos);
	}
	
	public void addExternalForce(VertexPos pos, double f0, double f1, double f2) {
//		VertexPos pos = new VertexPos(element, vertex);
		synchronized (vertexExternalforce) {
			vertexExternalforce.put(pos, vertexExternalforce.getOrDefault(pos, Vec3d.ZERO).addVector(f0, f1, f2));
		}
	}
	
	public void setElasticDeforming(BlockPos pos, IntPoint intPt, boolean elastic) {
		GlbIntPt glb = new GlbIntPt(pos, intPt);
		synchronized (elasticDeformation) {
			this.elasticDeformation.put(glb, elastic);
		}
	}
}
