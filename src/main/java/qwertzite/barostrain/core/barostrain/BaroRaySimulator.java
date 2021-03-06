package qwertzite.barostrain.core.barostrain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import qwertzite.barostrain.core.BSExplosionBase;
import qwertzite.barostrain.core.PressureRay;
import qwertzite.barostrain.core.common.coord.BlockFace;
import qwertzite.barostrain.core.fem.BlockPropProviderImpl;
import qwertzite.barostrain.core.fem.FEM;
import qwertzite.barostrain.core.fem.IBlockPropertyProvider;

public class BaroRaySimulator {

	private BaroStrainExplosion explosion;
	private World world;

	private FEM fem;
	
	private Map<BlockPos, Vec3d> affectedBlocks = new HashMap<>();
	
	public BaroRaySimulator(BaroStrainExplosion explosion) {
		this.explosion = explosion;
		this.world = explosion.getWorld();
	}
	
	public void evaluate() {
		IBlockPropertyProvider bpp = new BlockPropProviderImpl(this.explosion, this.explosion.getExploder(), this.world);
		fem = new FEM(bpp);
		
		Set<PressureRay> raySet; // 最初のrayを生成する
		raySet =PressureRay.seedRays(this.explosion.getPosition(), this.explosion.getIntencity(), this.world);
		// FIXME: internal destruction for the initial step
		while (!raySet.isEmpty()) {
			RayIter iter = this.rayLoop(raySet);
			Set<BlockPos> newlyDestroyeds = fem.femExec();
			
			raySet = Stream.concat(
					iter.getPendingRays().parallelStream().flatMap(ray -> ray.nextStep()),
					iter.getCollidedRays().entrySet().parallelStream().flatMap(e -> {
						BlockFace face = e.getKey();
						BlockPos pos = face.getBlockpos();
						Set<PressureRay> oldRay = e.getValue();
						
						double transmittance;
						if (!newlyDestroyeds.contains(pos)) { // not destroyed
							transmittance = 0.0d;
						} else {
							double initial = iter.getInitialForceFor(face);
							if (BSExplosionBase.isZero(initial)) { // too small pressure was applied.
								transmittance = 0.0d;
							} else {
								double remain = this.fem.getInertialForceFor(face);
								transmittance = MathHelper.clamp(remain / initial, 0, 1);
							}
						}
						return oldRay.parallelStream().flatMap(r -> r.reflection(transmittance));
					})).collect(Collectors.toSet());
			
			for (BlockPos pos : newlyDestroyeds) {
				synchronized (this.affectedBlocks) {
					this.affectedBlocks.put(pos, Vec3d.ZERO);
				}
				bpp.markAsStateChanged(pos);
				this.fem.markAsDestoyedBlock(pos);
			}
		}
	}
	
	private RayIter rayLoop(Set<PressureRay> raySet) {
		RayIter rayIter = new RayIter(this.fem);
		
		raySet.parallelStream().forEach(ray -> {
			RayTraceResult trace = this.internalCollision(ray);
			if (trace != null) {
				if (trace.typeOfHit == RayTraceResult.Type.BLOCK) rayIter.addHitRay(new BlockFace(trace.getBlockPos(), trace.sideHit), ray);
			} else {
				trace = this.externalCollision(ray);
				if (trace.typeOfHit == RayTraceResult.Type.BLOCK) rayIter.addHitRay(new BlockFace(trace.getBlockPos(), trace.sideHit), ray);
				else rayIter.addPendingRay(ray);
				// TODO: damage entity.
			}
		});
		return rayIter;
	}
	
	private RayTraceResult internalCollision(PressureRay ray) { // FIXME
		Vec3d from = ray.getAbsFrom();
		Vec3d to = ray.getAbsTo();
		@SuppressWarnings("unused")
		double pressure = ray.pressureAt(0.0d);
		BlockPos pos = new BlockPos(from);
		return this.pollBlockAt(pos, to, from, ray);
	}
	
	private RayTraceResult externalCollision(PressureRay ray) { // FIXME: check implementation is correct. Use ray tracing instead of polling.
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
	
	/**
	 * Add the amount of traversing blast to destroyed blocks.
	 * @param pos
	 * @param vec
	 */
	private synchronized void addBlastVec(BlockPos pos, Vec3d vec) { // keyの追加は破壊処理の時のみなので気にする必要はない
		this.affectedBlocks.put(pos, this.affectedBlocks.get(pos).add(vec));
	}
}
