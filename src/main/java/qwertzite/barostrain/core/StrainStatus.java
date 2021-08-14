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
public class StrainStatus {
	private BlockPos pos;
	private Axis axis;
	private double blastResistance = -1;
	private double hardness;
	
	private double absorved = 0.0d;
	/** その面が相手に及ぼしている力．<br>
	 * 正なら，その軸の正の向きの力を及ぼしている (自分は負の向きの反力を受けている)
	 */
	private EnumMap<EnumFacing, Double> transmittingForce = new EnumMap<>(EnumFacing.class);
	
	public StrainStatus(BlockPos pos, Axis axis, double resistance, double hardness) {
		this.pos = pos;
		this.axis = axis;
		this.blastResistance = resistance;
		this.hardness = hardness;
	}
	
	public BlockPos getPos() { return this.pos; }
	
	public double getAbsorveable(double applied) {
		return MathHelper.clamp(applied, this.blastResistance/10-this.absorved, -this.blastResistance / 10.0d - this.absorved);
	}
	
	/** どちらか一方の上限に達しているか */
	public boolean hasReachedCapacity() {
		return absorved <= -this.blastResistance / 10.0d + BSExplosionBase.ERR || this.blastResistance / 10.0d - BSExplosionBase.ERR <= absorved;
	}
	
	public boolean isElastoPasticDeforming(EnumFacing face) {
		double force = this.transmittingForce.getOrDefault(face, 0.0d);
		final double ERR = BSExplosionBase.ERR;
		if (face.getAxis() == this.axis) { // 軸力
			if (face.getAxisDirection() == AxisDirection.POSITIVE) { // 正なら圧縮，負なら引張
				return force >= this.blastResistance - ERR || force <= -this.hardness + ERR;
			} else {
				return force >= this.hardness - ERR        || force <= -this.blastResistance + ERR;
			}
		} else { // 剪断力
			return force <= - this.hardness / 2 + ERR || this.hardness / 2 - ERR <= force;
		}
	}
	
	public void absorveForce(double force) {
		this.absorved += force;
	}

	public double calcFlowableForceForFace(EnumFacing face, double applied) {
		double current = this.transmittingForce.getOrDefault(face, 0.0d);
		if (face.getAxis() == this.axis) { // 軸力
			if (face.getAxisDirection() == AxisDirection.POSITIVE) {
				return MathHelper.clamp(applied, this.blastResistance - current, -this.hardness - current); // 相手に正の向きの力を伝えるとき圧縮，負の向きの力を伝えるとき引張
			} else {
				return MathHelper.clamp(applied, this.hardness - current, -this.blastResistance - current);
			}
		} else { // 剪断力
			return MathHelper.clamp(applied, this.hardness / 2 - current, -this.hardness / 2 - current);
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
}
