package de.teundem.v2mplayer;

public class PlayerState {

	/*public class Channel {
		public int ptrnoteptr;
		public int notenr;
		public int notent;
		public byte lastnte;
		public byte lastvel;
		public int ptrpcptr;
		public int pcnr;
		public int pcnt;
		public byte lastpc;
		public int ptrpbptr;
		public int pbnr;
		public int pbnt;
		public byte lastpb0;
		public byte lastpb1;
		public CC[] ctl = new CC[] {new CC(), new CC(), new CC(), new CC(), new CC(), new CC(), new CC()};

	}*/

	/*public class CC {
		public int ptrccptr;
		public int ccnt;
		public int ccnr;
		public byte lastcc;

	}*/

	public static enum State {
		OFF, STOPPED, PLAYING
	};

	public State state;
	public int time;
	public int nexttime;
	public int ptrgptr;
	public int gnt;
	public int gnr;
	public int usecs;
	public int num;
	public int den;
	public int tpq;
	public int bar;
	public int beat;
	public int tick;

	public class Channel {
		public int ptrnoteptr;
		public int notenr;
		public int notent;
		public byte lastnte;
		public byte lastvel;
		public int ptrpcptr;
		public int pcnr;
		public int pcnt;
		public byte lastpc;
		public int ptrpbptr;
		public int pbnr;
		public int pbnt;
		public byte lastpb0;
		public byte lastpb1;

		public class CC {
			public int ptrccptr;
			public int ccnt;
			public int ccnr;
			public byte lastcc;

		}

		public CC[] ctl = new CC[] { new CC(), new CC(), new CC(), new CC(), new CC(), new CC(), new CC() };

	}

	public Channel[] chan = new Channel[] { new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel() };
	//public  Channel.CC[] ctl = new Channel.CC[7];
	public long cursmpl;
	public long smpldelta;
	public long smplrem;
	public int tdif;
}
