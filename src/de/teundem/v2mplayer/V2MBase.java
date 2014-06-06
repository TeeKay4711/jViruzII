package de.teundem.v2mplayer;

public class V2MBase {

	/*public class Channel {
		public int notenum;
		public int ptrnoteptr;
		public int pcnum;
		public int ptrpcptr;
		public int pbnum;
		public int ptrpbptr;
		public CC[] ctl = new CC[] {new CC(), new CC(), new CC(), new CC(), new CC(), new CC(), new CC()};
	}*/

	public class CC {
		public int ccnum;
		public int ptrccptr;
	}

	public boolean valid;
	public int ptrpatchmap;
	public int ptrglobals;
	public int timediv;
	public int timediv2;
	public int maxtime;
	public int ptrgptr;
	public int gdnum;

	public class Channel {
		public int notenum;
		public int ptrnoteptr;
		public int pcnum;
		public int ptrpcptr;
		public int pbnum;
		public int ptrpbptr;

		public class CC {
			public int ccnum;
			public int ptrccptr;
		}

		public CC[] ctl = new CC[] { new CC(), new CC(), new CC(), new CC(), new CC(), new CC(), new CC() };
	}

	public Channel[] chan = new Channel[] { new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel(), new Channel() };
	//public  Channel.CC ctl[] = new Channel.CC[7];
	public int ptrspeechdata;
	public String ptrspeechptrs[];
}
