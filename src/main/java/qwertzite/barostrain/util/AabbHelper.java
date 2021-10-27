package qwertzite.barostrain.util;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class AabbHelper {
	
	public static boolean isInside(AxisAlignedBB aabb, Vec3d vec) {
		return aabb.minX <= vec.x && vec.x <= aabb.maxX &&
				aabb.minY <= vec.y && vec.y <= aabb.maxY &&
				aabb.minZ <= vec.z && vec.z <= aabb.maxZ;
	}

	public static Vec3d computeIntercept(AxisAlignedBB aabb, Vec3d vecA, Vec3d vecB) {
		Vec3d vec3d = collideWithXPlane(aabb, aabb.minX, vecA, vecB);
//		EnumFacing enumfacing = EnumFacing.WEST;
		Vec3d vec3d1 = collideWithXPlane(aabb, aabb.maxX, vecA, vecB);
		if (vec3d1 != null && isClosest(vecA, vec3d, vec3d1)) {
			vec3d = vec3d1;
//			enumfacing = EnumFacing.EAST;
		}

		vec3d1 = collideWithYPlane(aabb, aabb.minY, vecA, vecB);
		if (vec3d1 != null && isClosest(vecA, vec3d, vec3d1)) {
			vec3d = vec3d1;
//			enumfacing = EnumFacing.DOWN;
		}
		vec3d1 = collideWithYPlane(aabb, aabb.maxY, vecA, vecB);
		if (vec3d1 != null && isClosest(vecA, vec3d, vec3d1)) {
			vec3d = vec3d1;
//			enumfacing = EnumFacing.UP;
		}

		vec3d1 = collideWithZPlane(aabb, aabb.minZ, vecA, vecB);
		if (vec3d1 != null && isClosest(vecA, vec3d, vec3d1)) {
			vec3d = vec3d1;
//			enumfacing = EnumFacing.NORTH;
		}
		vec3d1 = collideWithZPlane(aabb, aabb.maxZ, vecA, vecB);
		if (vec3d1 != null && isClosest(vecA, vec3d, vec3d1)) {
			vec3d = vec3d1;
//			enumfacing = EnumFacing.SOUTH;
		}

		return vec3d;
	}
	
	@VisibleForTesting
	private static boolean isClosest(Vec3d p_186661_1_, @Nullable Vec3d p_186661_2_, Vec3d p_186661_3_) {
		return p_186661_2_ == null || p_186661_1_.squareDistanceTo(p_186661_3_) < p_186661_1_.squareDistanceTo(p_186661_2_);
	}

	@Nullable
	@VisibleForTesting
	private static Vec3d collideWithXPlane(AxisAlignedBB aabb, double xpos, Vec3d vecA, Vec3d vecB) {
		Vec3d vec3d = vecA.getIntermediateWithXValue(vecB, xpos);
		return vec3d != null && aabb.intersectsWithYZ(vec3d) ? vec3d : null;
	}

	@Nullable
	@VisibleForTesting
	private static Vec3d collideWithYPlane(AxisAlignedBB aabb, double ypos, Vec3d vecA, Vec3d vecB) {
		Vec3d vec3d = vecA.getIntermediateWithYValue(vecB, ypos);
		return vec3d != null && aabb.intersectsWithXZ(vec3d) ? vec3d : null;
	}

	@Nullable
	@VisibleForTesting
	private static Vec3d collideWithZPlane(AxisAlignedBB aabb, double zpos, Vec3d vecA, Vec3d vecB) {
		Vec3d vec3d = vecA.getIntermediateWithZValue(vecB, zpos);
		return vec3d != null && aabb.intersectsWithXY(vec3d) ? vec3d : null;
	}

	@VisibleForTesting
	public boolean intersectsWithYZ(AxisAlignedBB aabb, Vec3d vec) {
		return vec.y >= aabb.minY && vec.y <= aabb.maxY && vec.z >= aabb.minZ && vec.z <= aabb.maxZ;
	}

	@VisibleForTesting
	public boolean intersectsWithXZ(AxisAlignedBB aabb, Vec3d vec) {
		return vec.x >= aabb.minX && vec.x <= aabb.maxX && vec.z >= aabb.minZ && vec.z <= aabb.maxZ;
	}

	@VisibleForTesting
	public boolean intersectsWithXY(AxisAlignedBB aabb, Vec3d vec) {
		return vec.x >= aabb.minX && vec.x <= aabb.maxX && vec.y >= aabb.minY && vec.y <= aabb.maxY;
	}
}
