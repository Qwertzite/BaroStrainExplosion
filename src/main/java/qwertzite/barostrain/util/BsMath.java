package qwertzite.barostrain.util;

import java.util.Random;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class BsMath {
	
	public static Vec3i add(Vec3i vd, Vec3i vi) {
		return new Vec3i(vd.getX() + vi.getX(), vd.getY() + vi.getY(), vd.getZ() + vi.getZ());
	}
	
	public static Vec3d add(Vec3d vd, Vec3i vi) {
		return vd.addVector(vi.getX(), vi.getY(), vi.getZ());
	}
	
	public static Vec3d add(Vec3d vd, Vec3d vi) {
		return vd.add(vi);
	}
	
	public static Vec3d subtract(Vec3d vd, Vec3i vi) {
		return vd.subtract(vi.getX(), vi.getY(), vi.getZ());
	}
	
	public static Vec3d subtract(Vec3i vi, Vec3d vd) {
		return new Vec3d(vi.getX()-vd.x, vi.getY()-vd.y, vi.getZ()-vd.z);
	}
	
	public static Vec3d subtract(Vec3d vd, Vec3d vi) {
		return vd.subtract(vi);
	}
	
	public static Vec3i mul(Vec3i vi, int x) {
		return new Vec3i(vi.getX()*x, vi.getY()*x, vi.getZ()*x);
	}
	
	public static Vec3d mul(Vec3d vd, double x) {
		return vd.scale(x);
	}
	
	public static double distance(Vec3i vi, Vec3d vd) {
		return MathHelper.sqrt(vi.distanceSq(vd.x, vd.y, vd.z));
	}
	
	public static double distance(Vec3d vi, Vec3d vd) {
		return vi.distanceTo(vd);
	}
	
	public static Vec3i invertX(Vec3i vd) {
		return new Vec3i(-vd.getX(), vd.getY(), vd.getZ());
	}
	
	public static Vec3i invertY(Vec3i vd) {
		return new Vec3i(vd.getX(), -vd.getY(), vd.getZ());
	}
	
	public static Vec3i invertZ(Vec3i vd) {
		return new Vec3i(vd.getX(), vd.getY(), -vd.getZ());
	}
	
	public static Vec3d invertX(Vec3d vd) {
		return new Vec3d(-vd.x, vd.y, vd.z);
	}
	
	public static Vec3d invertY(Vec3d vd) {
		return new Vec3d(vd.x, -vd.y, vd.z);
	}
	
	public static Vec3d invertZ(Vec3d vd) {
		return new Vec3d(vd.x, vd.y, -vd.z);
	}
	
	public static Vec3d[] invert3X(Vec3d[] vs) {
		return new Vec3d[] { invertX(vs[0]), invertX(vs[1]), invertX(vs[2]) };
	}
	public static Vec3d[] invert3Y(Vec3d[] vs) {
		return new Vec3d[] { invertY(vs[0]), invertY(vs[1]), invertY(vs[2]) };
	}
	public static Vec3d[] invert3Z(Vec3d[] vs) {
		return new Vec3d[] { invertZ(vs[0]), invertZ(vs[1]), invertZ(vs[2]) };
	}
	
	public static Vec3d average(Vec3d v0, Vec3d v1, Vec3d v2) {
		return new Vec3d((v0.x + v1.x + v2.x) / 3.0d, (v0.y + v1.y + v2.y) / 3.0d, (v0.z + v1.z + v2.z) / 3.0d);
	}
	
	public static Vec3d sumScale(Vec3d v0, Vec3d v1, double length) {
		double x = (v0.x + v1.x);
		double y = (v0.y + v1.y);
		double z = (v0.z + v1.z);
		double n = MathHelper.sqrt(x*x + y*y + z*z);
		x *= length/n;
		y *= length/n;
		z *= length/n;
		return new Vec3d(x, y, z);
	}
	
	public static long clamp(long num, long min, long max) {
		if (num < min) {
			return min;
		} else {
			return num > max ? max : num;
		}
	}
	
	public static int round(double x, Random rand) {
		return round(x, rand.nextDouble());
	}
	
	public static int round(double x, double rand) {
		int floor = MathHelper.floor(x);
		return floor + (rand <= x - floor ? 1 : 0);
	}
	

}
