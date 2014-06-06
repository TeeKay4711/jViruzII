package de.teundem.synth;

public class V2Delay {
	int pos, len;
	float[] buf;

	public void init(float[] buf, int len) {
		this.buf = buf;
		this.len = len;
		reset();
	}

	public void init(float[] buf) {
		this.init(buf, buf.length);
	}

	void reset() {
		//memset(buf, 0, sizeof(*buf)*len);
		pos = 0;
	}

	float fetch() {
		return buf[pos];
	}

	void feed(float v) {
		buf[pos] = v;
		if (++pos == len) {
			pos = 0;
		}
	}
}
