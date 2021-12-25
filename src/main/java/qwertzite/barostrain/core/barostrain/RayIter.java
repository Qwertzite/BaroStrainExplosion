package qwertzite.barostrain.core.barostrain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.util.EnumFacing;
import qwertzite.barostrain.core.PressureRay;
import qwertzite.barostrain.core.common.coord.BlockFace;
import qwertzite.barostrain.core.fem.FEM;

public class RayIter {
	
	private final FEM fem;
	private final Set<PressureRay> pendingRays;
	private final Map<BlockFace, Set<PressureRay>> collidedRays;
	/** 力の正の向きはその軸の正の向きとする */
	private final Object2DoubleMap<BlockFace> facePressure;
	
	public RayIter(FEM fem) {
		this.fem = fem;
		this.pendingRays = new HashSet<>();
		this.collidedRays = new HashMap<>();
		this.facePressure = new Object2DoubleOpenHashMap<>();
	}
	
	public void addPendingRay(PressureRay pressure) {
		synchronized (pendingRays) {
			this.pendingRays.add(pressure);
		}
	}
	
	/**
	 * 
	 * @param face
	 * @param ray
	 */
	public void addHitRay(BlockFace face, PressureRay ray) {
		double pressure = ray.getHitPressure();
		if (pressure <= 0) return;
		if (face.getFacing().getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE) pressure = -pressure;
		
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
		fem.applyPressure(face, pressure); // TODO: apply force to FEM object.
	}
	
	// ======== getter / setter ========
	
	public Set<PressureRay> getPendingRays() {
		return this.pendingRays;
	}
	
	public Map<BlockFace, Set<PressureRay>> getCollidedRays() {
		return this.collidedRays;
	}
	
	public double getInitialForceFor(BlockFace face) {
		return this.facePressure.getDouble(face);
	}
	
}
