package qwertzite.barostrain.core;

import java.util.EnumMap;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * あるブロックの爆破耐性，現在それぞれの面に掛かっている力，働いている慣性力を記録する<br>
 * また，爆破耐性，破壊耐性に基づき，それぞれの面があとどのくらいの力に耐えられるのかを計算する
 * 
 * @author qwertzite
 * @date 2021/04/01
 */
public class BlockStrain {
	private BlockPos pos;
	private Axis axis;
	private double blastResistance = -1;
	private double hardness;
	
	private double absorved = 0.0d;
	/** その面が相手に及ぼしている力．<br>
	 * 正なら，その軸の正の向きの力を及ぼしている (自分は負の向きの反力を受けている)
	 */
	private EnumMap<EnumFacing, Double> transmittingForce = new EnumMap<>(EnumFacing.class);
	
	public BlockStrain(BlockPos pos, Axis axis, double resistance, double hardness) {
		this.pos = pos;
		this.axis = axis;
		this.blastResistance = resistance;
		this.hardness = hardness;
	}
	
	public BlockPos getPos() { return this.pos; }
	
	public double getAbsorveable(double applied) {
		return MathHelper.clamp(applied, -this.blastResistance / 10.0d - this.absorved, this.blastResistance/10-this.absorved);
	}
	
	/** どちらか一方の上限に達しているか */
	public boolean hasReachedCapacity() {
		return absorved <= -this.blastResistance / 10.0d + BSExplosionBase.ERR || this.blastResistance / 10.0d - BSExplosionBase.ERR <= absorved;
	}
	
	public boolean isElastoPasticDeforming(EnumFacing face) {
		double force = this.transmittingForce.getOrDefault(face, 0.0d);
		final double ERR = BSExplosionBase.ERR;
		double maxLim = this.getMaxLim(face);
		double minLim = this.getMinLim(face);
		return force <= minLim + ERR || maxLim - ERR <= force;
	}
	
	public void absorveForce(double force) {
		this.absorved += force;
	}
	
	/**
	 * twice of that of non initial force.
	 * @param face
	 * @param applied
	 * @return
	 */
	public double calcFlowableInitialForceForFace(EnumFacing face, double applied) {
		double current = this.transmittingForce.getOrDefault(face, 0.0d);
		double maxLim = this.getMaxLim(face)*2 - current;
		double minLim = this.getMinLim(face)*2 - current;
//		System.out.println("flowable " + this.getPos() + " " + current + " " + this.getMinLim(face) + " " + this.getMaxLim(face) + " " + applied);
		return MathHelper.clamp(applied, minLim, maxLim);
	}

	public double calcFlowableForceForFace(EnumFacing face, double applied) {
		double current = this.transmittingForce.getOrDefault(face, 0.0d);
		double maxLim = this.getMaxLim(face) - current;
		double minLim = this.getMinLim(face) - current;
		return MathHelper.clamp(applied, minLim, maxLim);
	}
	
	private double getMaxLim(EnumFacing facing) {
		if (facing.getAxis() == this.axis) {
			return facing.getAxisDirection() == AxisDirection.POSITIVE ?
					this.blastResistance :
					this.hardness;
		} else {
			return this.hardness / 2;
		}
	}
	
	private double getMinLim(EnumFacing facing) {
		if (facing.getAxis() == this.axis) {
			return facing.getAxisDirection() == AxisDirection.POSITIVE ?
					- this.hardness :
					- this.blastResistance;
		} else {
			return - this.hardness / 2;
		}
	}

	/** 
	 * その面が (相手に) 及ぼしている力を加算する
	 * @param direction
	 * @param force
	 */
	public void flowForceThroughFace(EnumFacing direction, double force) {
		this.transmittingForce.put(direction, this.transmittingForce.getOrDefault(direction, 0.0d) + force);
	}
	
	public double getAbsorved() { return this.absorved; }
	
	public double getTransmittingForce(EnumFacing direction) {
		return this.transmittingForce.getOrDefault(direction, 0.0d);
	}
	
	public double getBlastResistance() { return this.blastResistance; }
	
	@Override
	public String toString() {
		return this.pos + ",res=" + this.blastResistance + ",hrd=" + this.hardness + ",abs=" + absorved + " " 
				+ "D:" + this.transmittingForce.getOrDefault(EnumFacing.DOWN, 0.0d) + " "
				+ "U:" + this.transmittingForce.getOrDefault(EnumFacing.UP, 0.0d) + " "
				+ "N:" + this.transmittingForce.getOrDefault(EnumFacing.NORTH, 0.0d) + " "
				+ "S:" + this.transmittingForce.getOrDefault(EnumFacing.SOUTH, 0.0d) + " "
				+ "W:" + this.transmittingForce.getOrDefault(EnumFacing.WEST, 0.0d) + " "
				+ "E:" + this.transmittingForce.getOrDefault(EnumFacing.EAST, 0.0d);
	}
}
