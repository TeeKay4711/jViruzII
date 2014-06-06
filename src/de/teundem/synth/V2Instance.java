package de.teundem.synth;

public class V2Instance implements Constants {
	static final int MAX_FRAME_SIZE = 280; // in samples

	float SRfcsamplesperms;
	float SRfcobasefrq;
	float SRfclinfreq;
	float SRfcBoostCos, SRfcBoostSin;
	float SRfcdcfilter;

	int SRcFrameSize;
	float SRfciframe;

	float[] vcebuf = new float[MAX_FRAME_SIZE];
	float[] vcebuf2 = new float[MAX_FRAME_SIZE];
	//StereoSample[] levelbuf = new StereoSample[MAX_FRAME_SIZE];
	//StereoSample[] chanbuf = new StereoSample[MAX_FRAME_SIZE];
	float[][] levelbuf = new float[MAX_FRAME_SIZE][2];
	float[][] chanbuf = new float[MAX_FRAME_SIZE][2];

	float[] aux1buf = new float[MAX_FRAME_SIZE];
	float[] aux2buf = new float[MAX_FRAME_SIZE];

	//StereoSample[] mixbuf = new StereoSample[MAX_FRAME_SIZE];
	//StereoSample[] auxabuf = new StereoSample[MAX_FRAME_SIZE];
	//StereoSample[] auxbbuf = new StereoSample[MAX_FRAME_SIZE];
	float[][] mixbuf = new float[MAX_FRAME_SIZE][2];
	float[][] auxabuf = new float[MAX_FRAME_SIZE][2];
	float[][] auxbbuf = new float[MAX_FRAME_SIZE][2];

	public void calcNewSampleRate(int samplerate) {
		float sr = (float) samplerate;
		SRfcsamplesperms = sr / 1000.0f;
		SRfcobasefrq = (fcoscbase * fc32bit) / sr;
		SRfclinfreq = fcsrbase / sr;
		SRfcdcfilter = 1.0f - fcdcflt / sr;

		SRcFrameSize = (int) (fcframebase * sr / fcsrbase + 0.5f);
		SRfciframe = 1.0f / (float) SRcFrameSize;

		assert (SRcFrameSize <= MAX_FRAME_SIZE);

		float boost = (fcboostfreq * fc2pi) / sr;
		SRfcBoostCos = (float) Math.cos(boost);
		SRfcBoostSin = (float) Math.sin(boost);
	}
}
