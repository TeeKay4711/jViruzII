package de.teundem.v2mplayer;

public abstract class IV2MPlayer {
	abstract public void Init();

	abstract public void Init(int a_tickspersec);

	abstract public boolean Open(int ptra_v2mptr);

	abstract public boolean Open(int ptra_v2mptr, int a_samplerate);

	abstract public void Close();

	abstract public void Play();

	abstract public void Play(int a_time);

	abstract public void Stop();

	abstract public void Stop(int a_fadetime);

	abstract public void Render(float[][] ptra_buffer, int a_len);

	abstract public void Render(float[][] ptra_buffer, int a_len, boolean a_add);

	//renderproxy
	abstract public boolean IsPlaying();

	//private V2MBase;
	//private PlayerState

	public byte m_synth[] = new byte[3 * 1024 * 1024];
	/*private*/static int m_tpc;
	/*private*/public static V2MBase m_base = new V2MBase();
	/*private*/public static PlayerState m_state = new PlayerState();
	/*private*/static int m_samplerate;
	/*private*/static long m_timeoffset;
	/*private*/static short m_midibuf[] = new short[4096];
	/*private*/static float m_fadeval;
	/*private*/static float m_fadedelta;

	abstract public boolean InitBase(int ptra_v2m);

	abstract public void Reset();

	abstract public void Tick();
}
