package de.teundem.synth;

import static de.teundem.synth.Utils.*;

class V2Osc implements Constants {
	static final int OSC_OFF = 0;
	static final int OSC_TRI_SAW = 1;
	static final int OSC_PULSE = 2;
	static final int OSC_SIN = 3;
	static final int OSC_NOISE = 4;
	static final int OSC_FM_SIN = 5;
	static final int OSC_AUXA = 6;
	static final int OSC_AUXB = 7;

	int mode; // OSC_*
	boolean ring; // ring modulation on/off
	long cnt; // wave counter
	int freq; // wave counter inc (8x/sample)
	long brpt; // break point for tri/pulse wave
	float nffrq, nfres; // noise filter freq/resonance
	long nseed; // noise random seed
	long oseed;
	float gain; // output gain
	V2LRC nf; // noise filter
	float note;
	float pitch;

	int state;
	
	V2Instance inst; // V2 instance we belong to.

	/* Debug stuff start */
	
	/*static boolean d_opened = false;
	static int d_count = 0;
	static boolean d_logged = false;
	static FileWriter writer;
	static PrintWriter print;*/
	
	/* Debug stuff end */

	void init(V2Instance instance, int idx) {
		final long[] seeds = { 0xdeadbeefL, 0xbaadf00dL, 0xd3adc0deL };
		//assert(idx < COUNTOF(seeds));
		nf = new V2LRC();
		cnt = 0l;
		nf.init();
		nseed = seeds[idx];
		inst = instance;
	}

	void noteOn() {
		chgPitch();
	}

	void chgPitch() {
		nffrq = inst.SRfclinfreq * calcfreq((pitch + 64.0f) / 128.0f);
		freq = (int) (inst.SRfcobasefrq * Math.pow(2.0f, (pitch + note - 60.0f) / 12.0f));
	}

	void set(syVOsc para) {
		assert brpt >= 0 && brpt <= 0xffffffffl;
		mode = (int) para.mode;
		ring = (((int) para.ring) & 1) != 0;

		pitch = (para.pitch - 64.0f) + (para.detune - 64.0f) / 128.0f;
		chgPitch();
		gain = para.gain / 128.0f;

		float col = para.color / 128.0f;
		brpt = ((long) ftou32(col) & 0xffffffffl);
		//brpt = ((long) ftou32(col));
		nfres = (float) (1.0f - Math.sqrt(col));
		
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
					int t = (ring) ? 1 : 0;

					print.printf("%d mode %d ring %d pitch %f gain %f col %f brpt %d nfres %f\n", d_count, mode, t, pitch, gain, col, brpt, nfres);
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

	}

	void render(float[] dest, int nsamples) {
		switch (mode & 7) {
			case OSC_OFF:
				break;
			case OSC_TRI_SAW:
				renderTriSaw(dest, nsamples);
				break;
			case OSC_PULSE:
				renderPulse(dest, nsamples);
				break;
			case OSC_SIN:
				renderSin(dest, nsamples);
				break;
			case OSC_NOISE:
				renderNoise(dest, nsamples);
				break;
			case OSC_FM_SIN:
				renderFMSin(dest, nsamples);
				break;
			case OSC_AUXA:
				renderAux(dest, inst.auxabuf, nsamples);
				break;
			case OSC_AUXB:
				renderAux(dest, inst.auxbbuf, nsamples);
				break;
		}

		/*System.out.printf("mode %d ring %b cnt %d freq %d brpt %d nffrq %f nfree %f nseed %d gain %f note %f pitch %f\n", mode, ring, cnt, freq, brpt, nffrq, nfres,
				nseed, gain, note, pitch);*/
		/*%d mode
		%b ring
		%d cnt
		%d freq
		%d brpt
		%f nffrq, %f nfres
		%d nseed
		%f gain
		%f note
		%f pitch*/

		//DEBUG_PLOT(this, dest, nsamples);
	}

	void output(float[] dest, int i, float x) {
		//System.out.printf("i %d x %f\n", i, x);
		if (ring)
			dest[i] *= x;
		else
			dest[i] += x;
	}

	// Oscillator state machine (read description of renderTriSaw for context)
	//
	// We keep track of whether the current sample is in the up or down phase,
	// whether the previous sample was, and if the waveform counter wrapped
	// around on the transition. This allows us to figure out which of
	// the cases above we fall into. Note this code uses a different bit ordering
	// from the ASM version that is hopefully a bit easier to understand.
	//
	// For reference: our bits map to the ASM version as follows (MSB.LSB order)
	//   (o)ld_up
	//   (c)arry
	//   (n)ew_up

	// carry:old_up:new_up
	static final int OSMTC_DOWN = 0; // old=down, new=down, no carry
	// 1 is an invalid configuration
	static final int OSMTC_UP_DOWN = 2; // old=up, new=down, no carry
	static final int OSMTC_UP = 3; // old=up, new=up, no carry
	static final int OSMTC_DOWN_UP_DOWN = 4; // old=down, new=down, carry
	static final int OSMTC_DOWN_UP = 5; // old=down, new=up, carry
	// 6 is an invalid configuration
	static final int OSMTC_UP_DOWN_UP = 7; // old=up, new=up, carry
	
	int osm_init() // our state field: old_up:new_up
	{
		long t = cnt - freq;
		if(t < 0l) {
			t = t & 0xffffffffl;
		}
		return (t < brpt) ? 3 : 0;
	}

	int osm_tick(int istate) // returns transition code
	{
		this.state = istate;
		assert cnt >= 0 && cnt <= 0xffffffffl : "V2Osc cnt out of bounds: " + cnt;

		// old_up = new_up, new_up = (cnt < brpt)
		this.state = ((this.state << 1) | ((cnt < brpt) ? 1 : 0)) & 3;

		// we added freq to cnt going from the previous sample to the current one.
		// so if cnt is less than freq, we carried.
		int transition_code = this.state | (cnt < freq ? 4 : 0);

		// finally, tick the oscillator
		//cnt += freq;
		long t = cnt + freq;
		if(t > 0xffffffffl) {
			t = t & 0xffffffffl;
		}
		cnt = t;
		
		//System.out.print(transition_code);
		return transition_code;
	}

	void renderTriSaw(float[] dest, int nsamples) {
		// Okay, so here's the general idea: instead of the classical sawtooth
		// or triangle waves, V2 uses a generalized triangle wave that looks like
		// this:
		//
		//       /\                  /\
		//      /   \               /   \
		//     /      \            /      \
		// ---/---------\---------/---------\> t
		//   /            \      /
		//  /               \   /
		// /                  \/
		// [-----1 period-----]
		// [-----] "break point" (brpt)
		//
		// If brpt=1/2 (ignoring fixed-point scaling), you get a regular triangle
		// wave. The example shows brpt=1/3, which gives an asymmetrical triangle
		// wave. At the extremes, brpt=0 gives a pure saw-down wave, and brpt=1
		// (if that was a possible value, which it isn't) gives a pure saw-up wave.
		//
		// Purely point-sampling this (or any other) waveform would cause notable
		// aliasing. The standard ways to avoid this issue are to either:
		// 1) Over-sample by a certain amount and then use a low-pass filter to
		//    (hopefully) get rid of the frequencies that would alias, or
		// 2) Generate waveforms from a Fourier series that's cut off below the
		//    Nyquist frequency, ensuring there's no aliasing to begin with.
		// V2 does neither. Instead it computes the convolution of the continuous
		// waveform with an analytical low-pass filter. The ideal low-pass in
		// terms of frequency response would be a sinc filter, which unfortunately
		// has infinite support. Instead, V2 just uses a simple box filter. This
		// doesn't exactly have favorable frequency-domain characteristics, but
		// it's still much better than point sampling and has the advantage that
		// it's fairly simple analytically. It boils down to computing the average
		// value of the waveform over the interval [t,t+h], where t is the current
		// time and h = 1/SR (SR=sampling rate), which is in turn:
		//
		//    f_box(t) = 1/h * (integrate(x=t..t+h) f(x) dx)
		//
		// Now there's a bunch of cases for these intervals [t,t+h] that we need to
		// consider. Bringing up the diagram again, and adding some intervals at the
		// bottom:
		//
		//       /\                  /\
		//      /   \               /   \
		//     /      \            /      \
		// ---/---------\---------/---------\> t
		//   /            \      /
		//  /               \   /
		// /                  \/
		// [-a-]      [-c]
		//     [--b--]       [-d--]
		//   [-----------e-----------]
		//          [-----------f-----------]
		//
		// a) is purely in the saw-up region,
		// b) starts in the saw-up region and ends in saw-down,
		// c) is purely in the saw-down region,
		// d) starts during saw-down and ends in saw-up.
		// e) starts during saw-up and ends in saw-up, but passes through saw-down
		// f) starts saw-down, ends saw-down, passes through saw-up.
		//
		// For simplicity here, I draw different-sized intervals sampling a fixed-
		// frequency wave, even though in practice it's the other way round, but
		// this way it's easier to put it all into a single picture.
		//
		// The original assembly code goes through a few gyrations to encode all
		// these possible cases into a bitmask and then does a single switch.
		// In practice, for all but very high-frequency waves, we're hitting the
		// "easy" cases a) and c) almost all the time.
		//COVER("Osc tri/saw");

		// calc helper values
		float f = utof23(freq);
		float omf = 1.0f - f;
		float rcpf = 1.0f / f;
		float col = utof23(brpt);

		// m1 = 2/col = slope of saw-up wave
		// m2 = -2/(1-col) = slope of saw-down wave
		// c1 = gain/2*m1 = gain/col = scaled integration constant
		// c2 = gain/2*m2 = -gain/(1-col) = scaled integration constant
		float c1 = gain / col;
		float c2 = -gain / (1.0f - col);

		this.state = osm_init();

		for (int i = 0; i < nsamples; i++) {
			float p = utof23(cnt & 0xffffffffl) - col;
			float y = 0.0f;

			// state machine action
			switch (osm_tick(this.state)) {
				case OSMTC_UP: // case a)
					// average of linear function = just sample in the middle
					y = c1 * (p + p - f);
					break;

				case OSMTC_DOWN: // case c)
					// again, average of a linear function
					y = c2 * (p + p - f);
					break;

				case OSMTC_UP_DOWN: // case b)
					y = rcpf * (c2 * sqr(p) - c1 * sqr(p - f));
					break;

				case OSMTC_DOWN_UP: // case d)
					y = -rcpf * (gain + c2 * sqr(p + omf) - c1 * sqr(p));
					break;

				case OSMTC_UP_DOWN_UP: // case e)
					y = -rcpf * (gain + c1 * omf * (p + p + omf));
					break;

				case OSMTC_DOWN_UP_DOWN: // case f)
					y = -rcpf * (gain + c2 * omf * (p + p + omf));
					break;

				// INVALID CASES
				default:
					assert (false);
					break;
			}

			output(dest, i, y + gain);
		}
	}

	void renderPulse(float[] dest, int nsamples) {
		// This follows the same general pattern as renderTriSaw above, except
		// this time the waveform is a pulse wave with variable pulse width,
		// which means we get very simple integrals. The state machine works
		// the exact same way, see above for description.
		//COVER("Osc pulse");

		// calc helper values
		float f = utof23(freq & 0xffffffffl);
		float gdf = gain / f;
		float col = utof23(brpt & 0xffffffffl);

		float cc121 = gdf * 2.0f * (col - 1.0f) + gain;
		float cc212 = gdf * 2.0f * col - gain;

		this.state = osm_init();

		for (int i = 0; i < nsamples; i++) {
			float p = utof23(cnt & 0xffffffffl);
			float out = 0.0f;

			switch (osm_tick(this.state)) {
				case OSMTC_UP:
					out = gain;
					break;

				case OSMTC_DOWN:
					out = -gain;
					break;

				case OSMTC_UP_DOWN:
					out = gdf * 2.0f * (col - p) + gain;
					break;

				case OSMTC_DOWN_UP:
					out = gdf * 2.0f * p - gain;
					break;

				case OSMTC_UP_DOWN_UP:
					out = cc121;
					break;

				case OSMTC_DOWN_UP_DOWN:
					out = cc212;
					break;

				// INVALID CASES
				default:
					assert (false);
					break;
			}

			output(dest, i, out);
		}
	}

	void renderSin(float[] dest, int nsamples) {
		//COVER("Osc sin");

		// Sine is already a perfectly bandlimited waveform, so we needn't
		// worry about aliasing here.
		for (int i = 0; i < nsamples; i++) {
			// Brace yourselves: The name is a lie! It's actually a cosine wave!
			long phase = (cnt + 0x40000000l) & 0xffffffffl; // quarter-turn (pi/2) phase offset
			cnt = (cnt + freq) & 0xffffffffl; // step the oscillator

			// range reduce to [0,pi]
			if (((phase & 0xffffffffl) & 0x80000000l) != 0) // Symmetry: cos(x) = cos(-x)
				phase = (~phase & 0xffffffffl); // V2 uses ~ not - which is slightly off but who cares

			// convert to t in [1,2)
			float t = bits2float(((phase & 0xffffffffl) >> 8) | 0x3f800000l); // 1.0f + (phase / (2^31))

			// and then to t in [-pi/2,pi/2)
			// i know the V2 ASM code says "scale/move to (-pi/4 .. pi/4)".
			// trust me, it's lying.
			t = t * fcpi - fc1p5pi;

			output(dest, i, gain * fastsin(t));
		}
	}

	void renderNoise(float[] dest, int nsamples) {
		//COVER("Osc noise");

		V2LRC flt = nf;
		long seed = nseed;

		for (int i = 0; i < nsamples; i++) {
			// uniform random value (noise)
			seed = urandom(seed);
			float n = frandom(seed);

			// filter
			float h = flt.step(n, nffrq, nfres);
			float x = nfres * (flt.l + h) + flt.b;

			output(dest, i, gain * x);
		}

		flt = nf;
		nseed = seed;
	}

	void renderFMSin(float[] dest, int nsamples) {
		//COVER("Osc FM");

		// V2's take on FM is a bit unconventional but fairly slick and flexible.
		// The carrier wave is always a sine, but the modulator is whatever happens
		// to be in the voice buffer at that point - which is the output of the
		// previous oscillators. So you can use all the oscillator waveforms
		// (including noise, other FMs and the aux bus oscillators!) as modulator
		// if you are so inclined.
		//
		// And it's very little code too :)
		for (int i = 0; i < nsamples; i++) {
			float mod = dest[i] * fcfmmax;
			float t = (utof23(cnt & 0xffffffffl) + mod) * fc2pi;
			cnt = (cnt + freq) & 0xffffffffl;

			float out = gain * fastsinrc(t);
			if (ring)
				dest[i] *= out;
			else
				dest[i] = out;
		}
	}

	void renderAux(float[] dest, float[][] src, int nsamples) {
		//COVER("Osc aux");

		float g = gain * fcgain;
		for (int i = 0; i < nsamples; i++) {
			float aux = g * (src[i][0] + src[i][1]);
			if (ring)
				aux *= dest[i];
			dest[i] = aux;
		}
	}
};
