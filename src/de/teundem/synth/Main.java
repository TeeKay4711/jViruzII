package de.teundem.synth;

import static de.teundem.synth.Utils.*;

public class Main {

	public static void main(String[] args) {
		//Utils.lSeed = 0xdeadbeefl;
		/*for(int i=0; i < 10; i++) {
			//int out = seed = urandom(seed);
			float out = frandom(Utils.lSeed);
			//Utils.seed = (int)out;
			//System.out.println(Integer.toHexString(out));
			System.out.println(out);
		}*/

		for (float i = -1.0f; i <= 1.0f; i = i + 0.01f) {
			//System.out.println(i+" "+fastatan(i));
			//System.out.println(i+" "+fastsin(i));
			System.out.println(i + " " + fastsinrc(i));
		}
		//System.out.println(seed & 0xffffffff);

	}

}
