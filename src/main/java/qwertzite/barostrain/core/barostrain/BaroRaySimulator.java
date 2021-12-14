package qwertzite.barostrain.core.barostrain;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import qwertzite.barostrain.core.PressureRay;
import qwertzite.barostrain.core.common.BlockFace;
import qwertzite.barostrain.core.fem.FEM;
import qwertzite.barostrain.core.fem.IBlockPropertyProvider;

public class BaroRaySimulator implements IBlockPropertyProvider {

	private BaroStrainExplosion explosion;
	private World world;

	FEM fem;
	
	private Map<BlockPos, Vec3d> affectedBlocks = new HashMap<>();
//	private Set<BlockFace>
	
	
	public BaroRaySimulator(BaroStrainExplosion explosion) {
		this.explosion = explosion;
		this.world = explosion.getWorld();
	}
	
	public void evaluate() {
		fem = new FEM(this);
		
		Set<PressureRay> raySet; // 最初のrayを生成する
		raySet =PressureRay.seedRays(this.explosion.getPosition(), this.explosion.getIntencity(), this.world);
		
		while (!raySet.isEmpty()) {
			raySet = this.rayLoop(raySet);
			fem.femExec();
			
			// TODO: process destroyed blocks.
		}
	}
	
	private Set<PressureRay> rayLoop(Set<PressureRay> raySet) {
		Set<PressureRay> pending = Collections.synchronizedSet(new HashSet<>());
		Map<BlockFace, Set<PressureRay>> hitRay = new HashMap<>();
		
		raySet.parallelStream().forEach(ray -> {
			RayTraceResult trace = this.internalCollision(ray);
			if (trace != null) {
				if (trace.typeOfHit == RayTraceResult.Type.BLOCK) this.addCollidingRay(trace.getBlockPos(), ray, true, hitRay);
			} else {
				trace = this.externalCollision(ray);
				if (trace.typeOfHit == RayTraceResult.Type.BLOCK) this.addCollidingRay(trace.getBlockPos(), ray, true, hitRay);
				else pending.add(ray);
				// TODO: damage entity.
			}
		});
		
		return pending; // TODO: add hit rays
	}
	
	private RayTraceResult internalCollision(PressureRay ray) { // FIXME
		Vec3d from = ray.getAbsFrom();
		Vec3d to = ray.getAbsTo();
		@SuppressWarnings("unused")
		double pressure = ray.pressureAt(0.0d);
		BlockPos pos = new BlockPos(from);
		return this.pollBlockAt(pos, to, from, ray);
	}
	
	private RayTraceResult externalCollision(PressureRay ray) { // FIXME
		Vec3d from = ray.getAbsFrom();
		Vec3d to = ray.getAbsTo();
		
		double dx = to.x - from.x;
		double dy = to.y - from.y;
		double dz = to.z - from.z;
		double dn = MathHelper.sqrt(dx*dx + dy*dy + dz*dz);
		double nx = dx/dn;
		double ny = dy/dn;
		double nz = dz/dn;
		
		double pressure = ray.pressureAt(0.0d);
		BlockPos pos = new BlockPos(from);
		
		for (double len = 0.3d; len < dn-0.1d; len += 0.3d) {
			pressure = ray.pressureAt(len);
			if (pressure <= 0.0d) {
				Vec3d hit = from.addVector(nx*len, ny*len, nz*len);
				return new RayTraceResult(RayTraceResult.Type.MISS, hit, null, new BlockPos(hit)); // 消滅した場合
			}
			int ix = MathHelper.floor(from.x + nx * len);
			int iy = MathHelper.floor(from.y + ny * len);
			int iz = MathHelper.floor(from.z + nz * len);
			if (ix == pos.getX() && iy == pos.getY() && iz == pos.getZ()) continue;
			pos = new BlockPos(ix, iy, iz);
			RayTraceResult trace = this.pollBlockAt(pos, from, to, ray);
			if (trace != null) {
				ray.setTraceResult(trace);
				return trace; // 衝突した場合
			}
		}
		
		pressure = ray.pressureAt(dn);
		if (pressure <= 0.0d) return new RayTraceResult(RayTraceResult.Type.MISS, to, null, new BlockPos(to)); // 消失した場合
		BlockPos lpos = new BlockPos(to);
		if (lpos.equals(pos)) {
			return new RayTraceResult(RayTraceResult.Type.MISS, to, null, new BlockPos(to)); // 衝突しなかった場合
		}
		
		RayTraceResult trace = this.pollBlockAt(lpos, from, to, ray);
		if (trace == null) {
			return new RayTraceResult(RayTraceResult.Type.MISS, to, null, new BlockPos(to)); // 衝突しなかった場合
		} else {
			ray.setTraceResult(trace);
			return trace; // 衝突した場合，
		}
	}
	
	/**
	 * If there is a destroyed block and ray intersects with the block collision BB, adds blast direction vector.<br>
	 * Else, returns a ray trace result if the ray intersects with the BB.
	 * @param blockpos
	 * @param from
	 * @param to
	 * @return
	 */
	private RayTraceResult pollBlockAt(BlockPos blockpos, Vec3d from, Vec3d to, PressureRay ray) {
		IBlockState iblockstate = this.world.getBlockState(blockpos);
		if (this.affectedBlocks.containsKey(blockpos)) { // add blast vector to block velocity.
			if (iblockstate.getBlock().canCollideCheck(iblockstate, true)) {
				RayTraceResult trace = iblockstate.collisionRayTrace(this.world, blockpos, from, to);
				if (trace != null) {
					double pressure = ray.pressureAt(trace.hitVec);
					if (pressure > 0.0d) {
						this.addBlastVec(blockpos, ray.getDirection().scale(pressure));
					}
				}
			}
			return null;
		}
		if ((iblockstate.getMaterial() == Material.PORTAL)
				|| iblockstate.getCollisionBoundingBox(this.world, blockpos) != Block.NULL_AABB) {
			if (iblockstate.getBlock().canCollideCheck(iblockstate, true)) {
				return iblockstate.collisionRayTrace(this.world, blockpos, from, to);
			}
		}
		return null;
	}
	
	private void addCollidingRay(BlockPos pos, PressureRay ray, boolean reversed, Map<BlockFace, Set<PressureRay>> hitRay) {
		double force = ray.getHitPressure();
		if (force <= 0) return;
		
		EnumFacing facing = ray.hit.sideHit;
		BlockFace bf = new BlockFace(pos, facing);
		synchronized (this) {
			if (hitRay.containsKey(bf)) hitRay.get(bf).add(ray);
			else {
				Set<PressureRay> set = new HashSet<>();
				hitRay.put(bf, set);
			}
		}
		fem.applyPressure(bf, force);
	}
	
	/**
	 * Add the amount of traversing blast to destroyed blocks.
	 * @param pos
	 * @param vec
	 */
	private synchronized void addBlastVec(BlockPos pos, Vec3d vec) { // keyの追加は破壊処理の時のみなので気にする必要はない
		this.affectedBlocks.put(pos, this.affectedBlocks.get(pos).add(vec));
	}
}
