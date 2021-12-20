package qwertzite.barostrain.core.fem;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import qwertzite.barostrain.core.common.coord.ElemVertex;
import qwertzite.barostrain.core.common.coord.IntPoint;

class ElemVertexShapeFuncPartial {
	
	@Test
	void test() {
		
//		for (ElemVertex ev : ElemVertex.values()) {
//			for (IntPoint ip : IntPoint.values()) {
//				for (int i = 0; i < 3; i++) {
//					System.out.println(ev + " " + ip + " " + i + " " + ev.shapeFuncPartial(i, ip));
//				}
//			}
//		}
		
		final double IP = 1 / Math.sqrt(3);
		System.out.println(test(ElemVertex.VNNN, 0, IntPoint.IP_PPP) + " " + testP0(-1, -1, -1, IP, IP, IP));
		System.out.println(test(ElemVertex.VPPP, 0, IntPoint.IP_PPP) + " " + testP0( 1,  1,  1, IP, IP, IP));
		System.out.println(test(ElemVertex.VNPP, 0, IntPoint.IP_PPP) + " " + testP0(-1,  1,  1, IP, IP, IP));
		System.out.println(test(ElemVertex.VNPP, 1, IntPoint.IP_PPP) + " " + testP1(-1,  1,  1, IP, IP, IP));
		System.out.println(test(ElemVertex.VNPP, 2, IntPoint.IP_PPP) + " " + testP2(-1,  1,  1, IP, IP, IP));
		
		assertTrue(true);
	}
	
	private double test(ElemVertex vertex, int axis, IntPoint ip) {
		return vertex.shapeFuncPartial(axis, ip);
	}
	
	private double testP0(int a, int b, int c, double xi, double eta, double zeta) {
		return a*(1+b*eta)*(1+c*zeta) / 8;
	}
	private double testP1(int a, int b, int c, double xi, double eta, double zeta) {
		return (1+a*xi)*b*(1+c*zeta) / 8;
	}
	private double testP2(int a, int b, int c, double xi, double eta, double zeta) {
		return (1+a*xi)*(1+b*eta)*c / 8;
	}
}
