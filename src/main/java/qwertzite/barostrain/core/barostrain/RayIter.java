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
	
	public void addHitRay(BlockFace face, PressureRay pressure) {
		
		// IMPL:
	}
}
