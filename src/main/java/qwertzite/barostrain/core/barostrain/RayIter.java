package qwertzite.barostrain.core.barostrain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import qwertzite.barostrain.core.PressureRay;
import qwertzite.barostrain.core.common.coord.BlockFace;

public class RayIter {
	
	public final Set<PressureRay> pendingRays;
	public final Map<BlockFace, Set<PressureRay>> collidedRays;
	/** 力の正の向きはその軸の正の向きとする */
	public final Object2DoubleMap<BlockFace> facePressure;
	
	public RayIter() {
		this.pendingRays = new HashSet<>();
		this.collidedRays = new HashMap<>();
		this.facePressure = new Object2DoubleOpenHashMap<>();
	}
	
	public void addPendingRay(PressureRay pressure) {
		this.pendingRays.add(pressure);
	}
	
	/**
	 * 
	 * @param face
	 * @param ray
	 * @param pressure positive pressure means that the face is pressed toward positive direction.
	 */
	public void addHitRay(BlockFace face, PressureRay ray, double pressure) {
		
		Set<PressureRay> set;
		synchronized (this.collidedRays) { // OPTIMIZE
			if (collidedRays.containsKey(face)) set = collidedRays.get(face);
			else collidedRays.put(face, set = new HashSet<>());
		}
		synchronized (set) {
			set.add(ray);
		}
		synchronized (this.facePressure) {
			this.facePressure.put(face, this.facePressure.getDouble(face) + pressure);
		}
		// TODO: write to FEM object.
	}
}
