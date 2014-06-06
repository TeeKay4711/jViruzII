package de.teundem.synth;

public interface Constants {
	// --------------------------------------------------------------------------
	// Constants.
	// --------------------------------------------------------------------------

	// Natural constants
	static final float fclowest = 1.220703125e-4f; // 2^(-13) - clamp EGs to 0 below this (their nominal range is 0..128) 
	static final float fcpi_2 = 1.5707963267948966192313216916398f;
	static final float fcpi = 3.1415926535897932384626433832795f;
	static final float fc1p5pi = 4.7123889803846898576939650749193f;
	static final float fc2pi =  6.28318530717958647692528676655901f;
	static final float fc32bit = 2147483648.0f; // 2^31 (original code has (2^31)-1, but this ends up rounding up to 2^31 anyway)

	// Synth constants
	static final float fcoscbase = 261.6255653f; // Oscillator base freq
	static final float fcsrbase = 44100.0f; // Base sampling rate
	static final float fcboostfreq = 150.0f; // Bass boost cut-off freq
	static final float fcframebase = 128.0f; // size of a frame in samples
	static final float fcdcflt = 126.0f;
	static final float fccfframe = 11.0f;

	static final float fcfmmax = 2.0f;
	static final float fcattackmul = -0.09375f; // -0.0859375
	static final float fcattackadd = 7.0f;
	static final float fcsusmul = 0.0019375f;
	static final float fcgain = 0.6f;
	static final float fcgainh = 0.6f;
	static final float fcmdlfomul = 1973915.49f;
	static final float fccpdfalloff = 0.9998f; // @@@BUG this should probably depend on sampling rate.

	static final float fcdcoffset = 3.814697265625e-6f; // 2^-18

}
