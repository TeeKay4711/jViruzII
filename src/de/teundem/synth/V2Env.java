package de.teundem.synth;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class V2Env implements Constants {
	/*public static enum State {
		OFF,
		RELEASE,
		
		ATTACK,
		DECAY,
		SUSTAIN
	}*/

	static final int OFF = 0;
	static final int RELEASE = 1;

	static final int ATTACK = 2;
	static final int DECAY = 3;
	static final int SUSTAIN = 4;

	float out;
	float state;
	float val;
	float atd;
	float dcf;
	float sul;
	float suf;
	float ref;
	float gain;

	/* Debug stuff start */
	/*static boolean d_opened = false;
	static int d_count = 0;
	static boolean d_logged = false;
	static FileWriter writer;
	static PrintWriter print;*/
	/* Debug stuff end */

	V2Instance inst;

	public void init(V2Instance instance) {
		state = OFF;
		inst = instance;
	}

	void set(syVEnv para) {
		atd = (float) Math.pow(2.0f, para.ar * fcattackmul + fcattackadd);
		dcf = 1.0f - Utils.calcfreq2(1.0f - para.dr / 128.0f);
		sul = para.sl;
		suf = (float) Math.pow(2.0f, fcsusmul * (para.sr - 64.0f));
		ref = 1.0f - Utils.calcfreq2(1.0f - para.rr / 128.0f);
		gain = para.vol / 128.0f;

		/* Debug writer START */
		/*try {
		if(!d_logged) {
			if(d_count < 1000)
			{
				if(!d_opened){
					//openFile
					writer = new FileWriter("DebugJ.txt");
					print = new PrintWriter(writer);
					d_opened = true;
				} else {
					d_count++;
					
					//System.out.printf("%d atd %f dcf %f sul %f suf %f ref %f gain %f", d_count, atd, dcf, sul, suf, ref, gain);
					//System.out.printf("\n");
					print.printf("%d atd %f dcf %f sul %f suf %f ref %f gain %f\n", d_count, atd, dcf, sul, suf, ref, gain);
				}
			} else {
				print.close();
				System.out.println("Closed");
				d_logged = true;
			}
		}
		} catch (IOException e){
			System.out.println("DebugOutputError");
		}*/
		/* Debug writer END */
	}

	void tick(boolean gate) {
		if (state <= RELEASE && gate) {
			state = ATTACK;
		} else if (state >= ATTACK && !gate) {
			state = RELEASE;
		}

		switch ((int) state) {
			case OFF:
				val = 0.0f;
				break;
			case ATTACK:
				val += atd;
				if (val >= 128.0f) {
					val = 128.0f;
					state = DECAY;
				}
				break;

			case DECAY:
				val *= dcf;
				if (val <= sul) {
					val = sul;
					state = SUSTAIN;
				}
				break;

			case SUSTAIN:
				val *= suf;
				if (val > 128.0f) {
					val = 128.0f;
				}
				break;

			case RELEASE:
				val *= ref;
				break;
		}

		if (val <= fclowest) {
			val = 0.0f;
			state = OFF;
		}

		out = val * gain;
	}
}
