package qwertzite.barostrain.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import qwertzite.barostrain.util.BsxMath;
import qwertzite.barostrain.util.BsxModLog;

public class PressureRay {
//	private static final int CHILD_SHIFT = 1;
//	private static final Vec3i[] CHILDS = new Vec3i[] {
//			new Vec3i(0, 1, 1), new Vec3i(0, 1, -1), new Vec3i(0, -1, -1), new Vec3i(0, -1, 1),
//			new Vec3i(1, 0, 1), new Vec3i(1, 0, -1), new Vec3i(-1, 0, -1), new Vec3i(-1, 0, 1),
//			new Vec3i(1, 1, 0), new Vec3i(1, -1, 0), new Vec3i(-1, -1, 0), new Vec3i(-1, 1, 0)};
	private static final Vec3d[][] ICOSAHEDRON;
	
	static {
		double a = 1.0d / MathHelper.sqrt(5.0d);
		double b = (1.0d - a) / 2.0d;
		double c = (1.0d + a) / 2.0d;
		double d = MathHelper.sqrt(b);
		double e = MathHelper.sqrt(c);
		Vec3d v0p = new Vec3d(0, 1, 0);
		Vec3d vp0 = new Vec3d(-2.0d*a, a, 0);
		Vec3d vp1 = new Vec3d(     -b, a, e);
		Vec3d vp2 = new Vec3d(      c, a, d);
		Vec3d vp3 = new Vec3d(      c, a,-d);
		Vec3d vp4 = new Vec3d(     -b, a,-e);
		Vec3d vn0 = vp0.subtractReverse(Vec3d.ZERO);
		Vec3d vn1 = vp1.subtractReverse(Vec3d.ZERO);
		Vec3d vn2 = vp2.subtractReverse(Vec3d.ZERO);
		Vec3d vn3 = vp3.subtractReverse(Vec3d.ZERO);
		Vec3d vn4 = vp4.subtractReverse(Vec3d.ZERO);
		Vec3d v0n = new Vec3d(0, -1, 0);
		ICOSAHEDRON = new Vec3d[][] {
			{ v0p, vp0, vp1 }, { v0p, vp1, vp2 }, { v0p, vp2, vp3 }, { v0p, vp3, vp4 }, { v0p, vp4, vp0 },
			{ vn2, vn3, vp0 }, { vp0, vp1, vn3 },
			{ vn3, vn4, vp1 }, { vp1, vp2, vn4 },
			{ vn4, vn0, vp2 }, { vp2, vp3, vn0 },
			{ vn0, vn1, vp3 }, { vp3, vp4, vn1 },
			{ vn1, vn2, vp4 }, { vp4, vp0, vn2 },
			{ vn2, vn3, v0n }, { vn3, vn4, v0n }, { vn4, vn0, v0n }, { vn0, vn1, v0n }, { vn1, vn2, v0n }
			};
	}
	
	private World world;
	private double intencity;
	private boolean initial;
	
	private double division; // multiply this to obtain actual force. ???????????????
	private double radius;
	public Vec3d origin;
	private Vec3d posPrev;
	public Vec3d[] seed;
	private Vec3d posNext;
	/** Distance travelled from the origin to {@code prevPos} */
	private double dist; // cumulative distance.
	
	public RayTraceResult hit;
	private double distFromPrevToHit;
	private double hitForce = -1;
	
	public PressureRay(float intencity, float division, Vec3d origin, Vec3d[] seed, World world) {
		this(intencity, division, 1.0d, origin, Vec3d.ZERO, seed, world, 0.0d);
	}
	
	public PressureRay(double intencity, double division, double radius, Vec3d origin, Vec3d posPrev, Vec3d[] seed, World world, double dist) {
		this.world = world;
		this.intencity = intencity;
		this.radius = radius;
		this.division = division;
		this.origin = origin;
		this.posPrev = posPrev;
		this.seed = seed;
		this.posNext = BsxMath.average(seed[0], seed[1], seed[2]);
		this.dist = dist;
//		RAYS.add(this);
	}
	public static final Set<PressureRay> RAYS = Collections.synchronizedSet(new HashSet<>());
	
	public static Set<PressureRay> seedRays(Vec3d origin, float intencity, World world) {
		float division = 1.0f * 26f; // ??????????????? ?????????????????????????????????????????? 20 ~ 
		return Stream.of(ICOSAHEDRON).parallel()
				.map(vs -> new PressureRay(intencity, division, origin, vs, world).setAsInitial())
				.collect(Collectors.toSet());
	}
	
	public PressureRay setAsInitial() { 
		this.initial = true;
		return this;
	}
	
	// ==== step 1: Ray trace????????????????????? ====
	
	public void setTraceResult(RayTraceResult result) {
		this.hit = result;
		this.distFromPrevToHit = this.distFromPrev(result.hitVec);
		this.hitForce =  this.pressureAt(result.hitVec);
		if (this.hitForce < 0.0d) this.hitForce = 0.0d;
	}
	
	// ==== step 3: ??????????????????????????? ====
	
	/**
	 * The non-collided rays are divided and returned for the next step.
	 * @return Stream of next divided rays.
	 */
	public Stream<PressureRay> nextStep() {
		double dist = BsxMath.distance(this.posNext, this.posPrev);
		if (this.pressureAt(dist) <= 0.0d) {
			return Stream.empty();
		}
		Vec3d prev = this.posNext;
		double radius = this.radius * 2.0d;
		Vec3d ns01 = BsxMath.sumScale(seed[0], seed[1], radius);
		Vec3d ns12 = BsxMath.sumScale(seed[1], seed[2], radius);
		Vec3d ns20 = BsxMath.sumScale(seed[2], seed[0], radius);
		Vec3d ns00 = seed[0].scale(2.0d);
		Vec3d ns11 = seed[1].scale(2.0d);
		Vec3d ns22 = seed[2].scale(2.0d);
		return Stream.of(
				new PressureRay(this.intencity, this.division/4, radius, this.origin, prev, new Vec3d[] { ns01, ns12, ns20 }, this.world, this.dist + dist),
				new PressureRay(this.intencity, this.division/4, radius, this.origin, prev, new Vec3d[] { ns00, ns01, ns20 }, this.world, this.dist + dist),
				new PressureRay(this.intencity, this.division/4, radius, this.origin, prev, new Vec3d[] { ns11, ns12, ns01 }, this.world, this.dist + dist),
				new PressureRay(this.intencity, this.division/4, radius, this.origin, prev, new Vec3d[] { ns22, ns20, ns12 }, this.world, this.dist + dist));
	}
	
	// reflection:  ???????????????1/10??????????????????????????????????????????
	public Stream<PressureRay> reflection(double tranRate) {
		if (this.hitForce <= 0.0d) return Stream.empty();
		if (this.hit.hitVec.distanceTo(this.origin.add(posPrev)) <= 0.01d) return Stream.empty();
		
		double transmission = division * tranRate; // / (1.0d + resistance*0.1d);
		double reflection = division * (1.0d - tranRate);
		Vec3d relHit = this.hit.hitVec.subtract(this.origin);
		PressureRay trans;
		PressureRay refl;
		if (transmission > 0.0d) {
			trans = new PressureRay(this.intencity, transmission, this.radius, this.origin, relHit, this.seed, this.world, this.dist + this.distFromPrevToHit);
		} else {
			trans = null;
		}
		Vec3d origin; // x?????????????????????????????????
		Vec3d[] seeds; // x?????????
		switch (this.hit.sideHit.getAxis()) {
		case X:
			origin = this.origin.addVector(2*relHit.x, 0, 0);
			seeds = BsxMath.invert3X(this.seed);
			break;
		case Y:
			origin = this.origin.addVector(0, 2*relHit.y, 0);
			seeds = BsxMath.invert3Y(this.seed);
			break;
		case Z:
			origin = this.origin.addVector(0, 0, 2*relHit.z);
			seeds = BsxMath.invert3Z(this.seed);
			break;
		default:
		BsxModLog.warn("Unknown hit side axis! {}", this.hit.sideHit.getAxis());
		assert(false);
		origin = null;
		seeds = null;
		}
//		System.out.println("" + this.origin + " " + this.hit.hitVec + this.hit.sideHit.getAxis() + " " + origin);
		refl = new PressureRay(this.intencity, reflection, this.radius, origin, this.hit.hitVec.subtract(origin), seeds, this.world, this.dist + this.distFromPrevToHit);
		if (trans == null) {
			return Stream.of(refl);
		} else {
			return Stream.of(refl, trans);
		}
	}
	
	// ==== util ====
	
	public double distFromPrev(Vec3d absPos) {
		double dx = absPos.x - this.posPrev.x - this.origin.x;
		double dy = absPos.y - this.posPrev.y - this.origin.y;
		double dz = absPos.z - this.posPrev.z - this.origin.z;
		return MathHelper.sqrt(dx*dx + dy*dy + dz*dz);
	}

	public double pressureAt(Vec3d absPos) {
		return this.pressureAt(this.distFromPrev(absPos));
	}
	
	/**
	 * computers pressure at "distFromFrom" away from "from".
	 * From ??????distFromFrom???????????????????????????????????????????????????
	 * @param distFromFrom
	 * @return
	 */
	public double pressureAt(double distFromFrom) {
		final double P = MathHelper.sqrt(intencity);
		final double Q = 0.5d*P + 0.5d;
		final double x1 = 2*Q;
		final double x2 = (2 + MathHelper.SQRT_2) * Q;
		final double y1 = P*Math.exp(-2)*4*Q*Q;
		final double y2 = P*Math.exp(-2-MathHelper.SQRT_2) * (6+4*MathHelper.SQRT_2) * Q * Q;
		double res = (y2 - y1)/(x2 - x1)*(this.dist + distFromFrom - x1) + y1;
		if (res < 0) res = 0.0d;
		else res *= this.division;
		return res;
	}
	
	public Vec3d getAbsFrom() {
		return BsxMath.add(this.origin, this.posPrev);
	}
	
	public Vec3d getAbsTo() {
		return BsxMath.add(this.origin, this.posNext);
	}
//	
//	public RayTraceResult getTraceResult() {
//		return this.hit;
//	}
	
	public double getHitPressure() {
		return this.hitForce;
	}
	
	public Vec3d getDirection() {
		return BsxMath.subtract(this.posNext, this.posPrev).normalize();
	}
	
	public boolean isInitial() {
		return this.initial;
	}
}