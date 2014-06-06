package de.teundem.synth;

public class Utils implements Constants {

	static long lSeed;
	static int iSeed;
	
	static float bits2float(int i) {
		//System.out.println(i);
		float res = Float.intBitsToFloat(i);
		//System.out.println("f: "+res);
		return res;
	}

	static float bits2float(long i) {
		//System.out.println(i);
		float res = Float.intBitsToFloat((int) i);
		//float res = (float)Double.longBitsToDouble(i & 0xffffffffl);
		//System.out.println("f: "+res);
		return res;
	}

	public static float fastatan(float x) {
		// extract sign
		float sign = 1.0f;
		float r;
		float x2 = x * x;

		if (x < 0.0f) {
			sign = -1.0f;
			x = -x;
		}

		/*float coeffs[][] = {
				{1.0f, 0.43157974f,        1.0f, 0.05831938f, 0.76443945f,        0.0f},
				{-0.431597974f,       -1.0f, 0.05831938f,        1.0f, 0.76443945f, 1.57079633f}};*/
		if (x >= 1.0f) {
			//r = (0.43157974f * x2 + 1.0f) * x / ((0.76443945f * x2 + 0.05831938f) * x2 + 1.0f) + 0.0f;
			r = (-1.0f * x2 + -0.431597974f) * x / ((0.76443945f * x2 + 1.0f) * x2 + 0.05831938f) + 1.57079633f;
		} else {
			//r = (-1.0f * x2 + -0.431597974f) * x / ((0.76443945f * x2 + 1.0f) * x2 + 0.05831938f) + 1.57079633f;
			r = (0.43157974f * x2 + 1.0f) * x / ((0.76443945f * x2 + 0.05831938f) * x2 + 1.0f) + 0.0f;
		}

		return r * sign;
	}

	public static float fastsin(float x) {
		float x2 = x * x;
		return (((-0.00018542f * x2 + 0.0083143f) * x2 - 0.16666f) * x2 + 1.0f) * x;
	}

	public static float fastsinrc(float x) {
		x = x % fc2pi;

		/*if (x < 0.0f) {
			x += fc2pi;
		}*/

		if (x > fc1p5pi) {
			x -= fc2pi;
		} else if (x > fcpi_2) {
			x = fcpi - x;
		}

		return fastsin(x);
	}

	public static float calcfreq(float x) {
		return (float) Math.pow(2.0f, (x - 1.0f) * 10.0f);
	}

	public static float calcfreq2(float x) {
		return (float) Math.pow(2.0f, (x - 1.0f) * fccfframe);
	}

	public static float sqr(float x) {
		return x * x;
	}

	public static float min(float a, float b) {
		return (a < b) ? a : b;
	}

	public static float max(float a, float b) {
		return (a > b) ? a : b;
	}

	public static float clamp(float x, float min, float max) {
		return (x < min) ? min : (x > max) ? max : x;
	}

	public static long urandom(long seed) {
		//lSeed = seed;
		//lSeed = lSeed & 0xffffffffl;
		//lSeed = (lSeed * 196314165 + 907633515);
		//System.out.println("urandom: " + (lSeed & 0xffffffffl));
		//lSeed = (long)(iSeed);
		seed = (seed * 196314165 + 907633515) & 0xffffffffl;
		//lSeed = seed;
		long r = seed;
		//System.out.println(r);
		//return (int) (lSeed & 0xffffffff);
		return  r;
	}

	public static float frandom(long seed) {
		int bits = (int)seed; //(int) urandom(seed);
		//System.out.println(bits);
		float f = bits2float((bits >>> 9) | 0x40000000l);
		//System.out.println(f);
		float r = f - 3.0f;
		//return f - 3.0f;
		return r;
	}

	/*public static float utof23(int x) {
		float f = bits2float((x >>> 9) | 0x3f800000);
		return f - 1.0f;
	}

	public static float utof23(long x) {
		float f = bits2float((x >>> 9) | 0x3f800000l);
		return f - 1.0f;
	}*/
	
	public static float utof23(long x) {

		assert x >= 0l && x <= 0xffffffffl : "X ist ausserhalb normaler Paramater " + x;
		
		long t1 = (x << 20) | 0x3ff0000000000000l;
		double x1 = Double.longBitsToDouble(t1);
		
		return (float)(x1 - 1.0f);

	}


	/*public static int ftou32(long v) {
		
		return 2 * (int) (v * fc32bit);
	}*/

	public static long ftou32(float v) {

		assert v >= -1.0f && v <= 1.0f : "ftou32-error: v (" + v +") ausserhalb des gültigen bereiches...";

		return (2 * (int)(v * fc32bit)) & 0xffffffff;
	}

	public static float lerp(float a, float b, float t) {
		return a + t * (b - a);
	}
}
