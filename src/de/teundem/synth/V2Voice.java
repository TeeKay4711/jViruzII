package de.teundem.synth;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

class V2Voice implements Constants {
	final int FLTR_SINGLE = 0;
	final int FLTR_SERIAL = 1;
	final int FLTR_PARALLEL = 2;

	final int SYNC_NONE = 0;
	final int SYNC_OSC = 1;
	final int SYNC_FULL = 2;

	int note;
	float velo;
	boolean gate;

	float curvol;
	float volramp;

	float xpose; // transpose
	int fmode; // FLTR_*
	float lvol; // left volume
	float rvol; // right volume
	float f1gain; // filter 1 gain
	float f2gain; // filter 2 gain

	int keysync;

	V2Osc[] osc = new V2Osc[syVV2.NOSC];
	V2Flt[] vcf = new V2Flt[syVV2.NFLT];
	V2Env[] env = new V2Env[syVV2.NENV];
	V2LFO[] lfo = new V2LFO[syVV2.NLFO];
	V2Dist dist = new V2Dist(); // distorter
	V2DCFilter dcf = new V2DCFilter(); // post DC filter

	V2Instance inst;

	void init(V2Instance instance) {
		for (int i = 0; i < syVV2.NOSC; i++) {
			osc[i] = new V2Osc();
			osc[i].init(instance, i);
		}
		for (int i = 0; i < syVV2.NFLT; i++) {
			vcf[i] = new V2Flt();
			vcf[i].init(instance);
		}
		for (int i = 0; i < syVV2.NENV; i++) {
			env[i] = new V2Env();
			env[i].init(instance);
		}
		for (int i = 0; i < syVV2.NLFO; i++) {
			lfo[i] = new V2LFO();
			lfo[i].init(instance);
		}
		dist.init(instance);
		dcf.init(instance);
		inst = instance;
	}

	void tick() {
		for (int i = 0; i < syVV2.NENV; i++)
			env[i].tick(gate);

		for (int i = 0; i < syVV2.NLFO; i++)
			lfo[i].tick();

		// volume ramping slope
		volramp = (env[0].out / 128.0f - curvol) * inst.SRfciframe;
		//DEBUG_PLOT_VAL(curvol, curvol);
	}

	//void render(StereoSample[] dest, int nsamples) {
	void render(float[][] dest, int nsamples) {
		assert (nsamples <= V2Instance.MAX_FRAME_SIZE);

		float voice[] = inst.vcebuf;
		float voice2[] = inst.vcebuf2;
		//memset(voice, 0, nsamples * sizeof(*voice));

		// clear voice buffer
		for (int i = 0; i < nsamples; i++) {
			voice[i] = 0.0f;
			voice2[i] = 0.0f;
		}

		// oscillators -> voice buffer
		for (int i = 0; i < syVV2.NOSC; i++)
			osc[i].render(voice, nsamples);

		/*try {
			FileOutputStream fos = new FileOutputStream("test.raw", true);
			DataOutputStream dos = new DataOutputStream(fos);

			for (int j = 0; j < nsamples; j++) {
				dos.writeFloat(voice[j]);
			}
			dos.close();

		} catch (IOException e) {

		}*/

		// voice buffer -> filters -> voice buffer
		switch (fmode) {
			case FLTR_SINGLE:
				//COVER("VOICE filter single");
				vcf[0].render(voice, voice, nsamples);
				break;

			case FLTR_SERIAL:
			default:
				//COVER("VOICE filter serial");
				vcf[0].render(voice, voice, nsamples);
				vcf[1].render(voice, voice, nsamples);
				break;

			case FLTR_PARALLEL:
				//COVER("VOICE filter parallel");
				vcf[1].render(voice2, voice, nsamples);
				vcf[0].render(voice, voice, nsamples);
				for (int i = 0; i < nsamples; i++)
					voice[i] = voice[i] * f1gain + voice2[i] * f2gain;
				break;
		}

		// voice buffer . distortion . voice buffer
		dist.renderMono(voice, voice, nsamples);

		// voice buffer . dc filter . voice buffer
		dcf.renderMono(voice, voice, nsamples);

		//DEBUG_PLOT(this, voice, nsamples);

		// voice buffer (mono) . +=output buffer (stereo)
		// original ASM code has chan buffer hardwired as output here
		float cv = curvol;
		for (int i = 0; i < nsamples; i++) {
			float out = voice[i] * cv;
			cv += volramp;

			dest[i][0] += lvol * out + fcdcoffset;
			dest[i][1] += rvol * out + fcdcoffset;
		}

		/*try {
			FileOutputStream fos = new FileOutputStream("test.raw", true);
			DataOutputStream dos = new DataOutputStream(fos);

			for (int i = 0; i < nsamples; i++) {
				dos.writeFloat(dest[i][1]);
			}
			dos.close();

		} catch (IOException e) {

		}*/

		curvol = cv;
	}

	void set(syVV2 para) {
		xpose = para.transp - 64.0f;
		updateNote();

		fmode = (int) para.routing;
		keysync = (int) para.oscsync;

		// equal power panning
		float p = para.panning / 128.0f;
		lvol = (float) Math.sqrt(1.0f - p);
		rvol = (float) Math.sqrt(p);

		// filter balance for parallel
		float x = (para.fltbal - 64.0f) / 64.0f;
		if (x >= 0.0f) {
			f2gain = 1.0f;
			f1gain = 1.0f - x;
		} else {
			f1gain = 1.0f;
			f2gain = 1.0f + x;
		}

		// subsections
		for (int i = 0; i < syVV2.NOSC; i++) {
			osc[i].set(para.osc[i]);
			//System.out.printf("%f %f %f %f %f %f\n", para.osc[i].mode, para.osc[i].ring, para.osc[i].pitch, para.osc[i].detune, para.osc[i].color, para.osc[i].gain);
		}
		for (int i = 0; i < syVV2.NENV; i++)
			env[i].set(para.env[i]);

		for (int i = 0; i < syVV2.NFLT; i++)
			vcf[i].set(para.flt[i]);

		for (int i = 0; i < syVV2.NLFO; i++)
			lfo[i].set(para.lfo[i]);

		dist.set(para.dist);
	}

	void noteOn(int note, int vel) {
		this.note = note;
		updateNote();

		velo = (float) vel;
		gate = true;

		// reset EGs
		for (int i = 0; i < syVV2.NENV; i++)
			env[i].state = V2Env.ATTACK;

		// process sync
		switch (keysync) {
			case SYNC_FULL:
				//COVER("VOICE noteOn sync full");
				for (int i = 0; i < syVV2.NENV; i++)
					env[i].val = 0.0f;
				curvol = 0.0f;

				for (int i = 0; i < syVV2.NOSC; i++)
					osc[i].init(inst, i);

				for (int i = 0; i < syVV2.NFLT; i++)
					vcf[i].init(inst);

				dist.init(inst);
				// fall-through

			case SYNC_OSC:
				//COVER("VOICE noteOn sync osc");
				for (int i = 0; i < syVV2.NOSC; i++)
					osc[i].cnt = 0;
				// fall-through

			case SYNC_NONE:
			default:
				break;
		}

		for (int i = 0; i < syVV2.NOSC; i++)
			osc[i].chgPitch();

		for (int i = 0; i < syVV2.NLFO; i++)
			lfo[i].keyOn();

		dcf.init(inst);
	}

	void noteOff() {
		gate = false;
	}

	void updateNote() {
		float n = xpose + (float) note;
		for (int i = 0; i < syVV2.NOSC; i++)
			osc[i].note = n;
	}
};
