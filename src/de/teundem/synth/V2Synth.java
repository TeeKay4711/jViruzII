package de.teundem.synth;

import static de.teundem.v2m.Main.*;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

public class V2Synth {
	public static final int POLY = 64;
	public static final int CHANS = 16;

	public int patchmap;

	int mrstat;
	int curalloc;
	int samplerate;
	int chanmap[] = new int[POLY];
	int allocpos[] = new int[POLY];
	int voicemap[] = new int[CHANS];
	int tickd;
	V2ChanInfo chans[] = new V2ChanInfo[CHANS];
	syVV2 voicesv[] = new syVV2[POLY];
	V2Voice voicesw[] = new V2Voice[POLY];
	syVChan chansv[] = new syVChan[CHANS];
	V2Chan chansw[] = new V2Chan[CHANS];

	public class Globals {
		public syVReverb rvbparm = new syVReverb();
		public syVModDel delparm = new syVModDel();
		public float vlowcut;
		public float vhighcut;
		public syVComp cprparm = new syVComp();
		public byte guicolor;
		public byte _pad[] = new byte[3];
	}

	Globals globals = new Globals();
	V2Reverb reverb = new V2Reverb();
	V2ModDel delay = new V2ModDel();
	V2DCFilter dcf = new V2DCFilter();
	V2Comp compr = new V2Comp();
	float lcfreq;
	float lcbuf[] = new float[2];
	float hcfreq;
	float hcbuf[] = new float[2];

	boolean initialized;

	float maindelbuf[][] = new float[2][32768];
	float chandelbuf[][][] = new float[CHANS][2][2048];

	/* Debug stuff start */

	/*static boolean d_opened = false;
	static int d_count = 0;
	static boolean d_logged = false;
	static FileWriter writer;
	static PrintWriter print;*/

	/* Debug stuff end */

	V2Instance instance = new V2Instance();

	//syWRonan ronan;

	public void init(int patchmap, int samplerate) {
		//memset(this, 0, sizeof(this))

		this.samplerate = samplerate;
		instance.calcNewSampleRate(samplerate);
		//ronanCBSetSR(&ronan, samplerate);

		this.patchmap = patchmap;

		for (int i = 0; i < POLY; i++) {
			voicesv[i] = new syVV2();
			chanmap[i] = -1;
			voicesw[i] = new V2Voice();
			voicesw[i].init(instance);
		}

		for (int i = 0; i < CHANS; i++) {
			chansv[i] = new syVChan();
			chans[i] = new V2ChanInfo();
			chans[i].ctl[6] = 0x7f;
			chansw[i] = new V2Chan();
			//chansw[i].init(instance, chandelbuf[i][0], chandelbuf[i][1], COUNTOF(chandelbuf[i][0]));
			chansw[i].init(instance, chandelbuf[i][0], chandelbuf[i][1], chandelbuf[i][0].length);
		}

		reverb.init(instance);
		//delay.init(instance, maindelbuf[0], maindelbuf[1], COUNTOF(maindelbuf[0]));
		delay.init(instance, maindelbuf[0], maindelbuf[1], maindelbuf[0].length);
		//ronanCBInit(&ronan);
		compr.init(instance);
		dcf.init(instance);

		initialized = true;
	}

	public void render(int bufp, float[][] buf, long nsamples, float buf2, boolean add) {
	//public void render(ByteBuffer buf, long nsamples, float buf2, boolean add) {
		//System.out.println("Rendering " + nsamples + " samples...");
		long todo = nsamples;
		int bufpp = 0;
		while (todo != 0) {
			if (tickd == 0) {
				tick();
			}

			//StereoSample[] src = instance.mixbuf; //[instance.SRcFrameSize - tickd];
			bufpp = instance.SRcFrameSize - tickd;
			//System.out.printf("%d %d \n", bufp, bufpp);
			float[][] src = instance.mixbuf;

			long nread = Math.min(todo, tickd);

			if (buf2 == 0) {
				if (!add) {
					for(int i = 0; i < nread; i++) {
						buf[bufp + i][0] = src[bufpp + i][0];
						//src[i][0] = 0.5f;
						buf[bufp + i][1] = src[bufpp + i][1];
					}
					//memcpy(buf, src, nread * sizeof(StereoSample));
					//System.out.println("Write buffer" + todo);
				} else {
					for (int i = 0; i < nread; i++) {
						//buf[i*2+0] = src[i].l;
						//buf[i*2+1] = src[i].r;
					}
				}
				//buf += 2 * nread;
				bufp += nread;
				//System.out.printf("bufp %d nread %d \n", bufp, nread);

			} else {
				if (!add) {
					for (int i = 0; i < nread; i++) {
						//buf[i] = src[i].l;
						//buf2[i] = src[i].r;
					}
				} else {
					for (int i = 0; i < nread; i++) {
						//buf[i] += src[i].l;
						//buf2[i] += src[i].r;
					}
				}

				//buf += nread;
				//buf2 += nread;
			}

			todo -= nread;
			tickd -= nread;
		}
		
		//System.out.printf("todo %d tickd %d bufp %d \n", todo, tickd, bufp);
	}

	public void processMIDI(short[] cmd) {
		int ptr = 0;
		//short midibyte = (short) (cmd[ptr] & 0xff);
		/*for(short d : cmd) {
			System.out.printf("%x ", d);
			if(d == 0xfd) break;
		}
		System.out.printf("\n");*/
		while (cmd[ptr] != 0xfd) {
			//System.out.println(midibyte);

			if ((cmd[ptr] & 0x80) != 0) {
				mrstat = cmd[ptr++];
				//midibyte = (short) (cmd[++ptr] & 0xff);
				//System.out.println(mrstat + ": Start of message");
			}

			if (mrstat < 0x80) {
				//System.out.println(mrstat + ": We don't have a current message? uhm...");
				break;
			}

			int chan = mrstat & 0xf;

			switch ((mrstat >> 4) & 7) {

			/*
			 * Note On
			 */

				case 1:
					//System.out.println("Case 1: Note On");
					if (cmd[ptr + 1] != 0) {
						if (chan == CHANS - 1) {
							//ronanCBNoteOn(&ronan)
						}

						V2Sound sound = getpatch(chans[chan].pgm);
						int npoly = 0;
						for (int i = 0; i < POLY; i++) {
							//npoly += (chanmap[i] == chan);
							if (chanmap[i] == chan)
								npoly++;
						}

						int usevoice = -1;
						int chanmask, chanfind;

						if (npoly != 0 || npoly < sound.maxpoly) {
							for (int i = 0; i < POLY; i++) {
								if (chanmap[i] < 0) {
									usevoice = i;
									break;
								}
							}

							chanmask = 0;
							chanfind = 0;
						} else {
							chanmask = 0xf;
							chanfind = chan;
						}

						if (usevoice < 0) {
							int oldest = curalloc;
							for (int i = 0; i < POLY; i++) {
								if ((chanmap[i] & chanmask) == chanfind && !voicesw[i].gate && allocpos[i] < oldest) {
									oldest = allocpos[i];
									usevoice = i;
								}
							}
						}

						if (usevoice < 0) {
							int oldest = curalloc;
							for (int i = 0; i < POLY; i++) {
								if ((chanmap[i] & chanmask) == chanfind && allocpos[i] < oldest) {
									oldest = allocpos[i];
									usevoice = i;
								}
							}
						}

						chanmap[usevoice] = chan;
						voicemap[chan] = usevoice;
						allocpos[usevoice] = curalloc++;

						storeV2Values(usevoice);
						int x = cmd[ptr + 0] & 0xff;
						int y = cmd[ptr + 1] & 0xff;
						ptr += 2;
						/**///System.out.println("Chan "+chan+" Note " + x + " Vel " + y);
						voicesw[usevoice].noteOn(x, y);
						break;
					}
					/*
					 * Note Off
					 */
				case 0:
					/**///System.out.printf("Chan %d NoteOff\n", chan);
					if (chan == CHANS - 1) {
						//ronanCBNoteOff(&ronan)
					}

					for (int i = 0; i < POLY; i++) {
						if (chanmap[i] != chan) {
							continue;
						}

						V2Voice voice = voicesw[i];
						if (voice.note == cmd[ptr] && voice.gate == true) {
							voice.noteOff();
							//System.out.println("Note Off");
						}
					}
					ptr += 2;
					break;
				/*
				 * Aftertouch	
				 */
				case 2:
					/**///System.out.printf("Chan %d Aftertouch", chan);
					ptr++;
					break;
				/*
				 * Control Change
				 */
				case 3:
					//System.out.println("Case 3");
					int ctrl = cmd[ptr + 0];
					short val = cmd[ptr + 1];
					/**///System.out.printf("Chan %d Ctrl %d Val %d\n", chan, ctrl, val);
					if (ctrl >= 1 && ctrl <= 7) {
						chans[chan].ctl[ctrl - 1] = val;
						if (chan == CHANS - 1) {
							//ronanCBSetCtl(&ronan, ctrl, val
						}
					} else if (ctrl == 120) {
						for (int i = 0; i < POLY; i++) {
							if (chanmap[i] != chan) {
								continue;
							}

							voicesw[i].init(instance);
							chanmap[i] = -1;
						}
					} else if (ctrl == 123) {
						if (chan == CHANS - 1) {
							//ronanCBNoteOff(&ronan);
						}

						for (int i = 0; i < POLY; i++) {
							if (chanmap[i] == chan) {
								voicesw[i].noteOff();
								//System.out.println("Note Off");
							}
						}
					}
					ptr += 2;
					break;
				/*
				 * Program Change
				 */
				case 4: {
					//System.out.println("Case 4: MIDI Program Change");
					short pgm = (short) (cmd[ptr++] & 0x7f);
					/**///System.out.println("Chan "+chan+" Pgm: "+pgm);
					if (chans[chan].pgm != pgm) {
						chans[chan].pgm = pgm;

						for (int i = 0; i < POLY; i++) {
							if (chanmap[i] == chan) {
								chanmap[i] = -1;
							}
						}
					}
					for (int i = 0; i < 6; i++) {
						chans[chan].ctl[i] = 0;
					}
				}
					break;
				/*
				 * Pitch Bend
				 */
				case 5:
					//System.out.println("Case 5");
					ptr += 2;
					break;
				/*
				 * Poly Aftertouch
				 */
				case 6:
					//System.out.println("Case 6");
					ptr += 2;
					break;
				/*
				 * System Exclusive
				 */
				case 7:
					//System.out.println("Case 6");
					if (chan == 0xf) {
						init(patchmap, samplerate);
					}
					break;
			}
		}
	}

	public void setGlobals(int para) {
		if (!initialized) {
			return;
		}

		byte[] globalParams = new byte[23];
		v2mBuffer.rewind();
		v2mBuffer.position(para);
		v2mBuffer.get(globalParams);

		globals.rvbparm.revtime = globalParams[0];
		globals.rvbparm.highcut = globalParams[1];
		globals.rvbparm.lowcut = globalParams[2];
		globals.rvbparm.vol = globalParams[3];
		globals.delparm.amount = globalParams[4];
		globals.delparm.fb = globalParams[5];
		globals.delparm.llength = globalParams[6];
		globals.delparm.rlength = globalParams[7];
		globals.delparm.mrate = globalParams[8];
		globals.delparm.mdepth = globalParams[9];
		globals.delparm.mphase = globalParams[10];
		globals.vlowcut = globalParams[11];
		globals.vhighcut = globalParams[12];
		globals.cprparm.mode = globalParams[13];
		globals.cprparm.stereo = globalParams[14];
		globals.cprparm.autogain = globalParams[15];
		globals.cprparm.lookahead = globalParams[16];
		globals.cprparm.threshold = globalParams[17];
		globals.cprparm.ratio = globalParams[18];
		globals.cprparm.attack = globalParams[19];
		globals.cprparm.release = globalParams[20];
		globals.cprparm.outgain = globalParams[21];
		globals.guicolor = globalParams[22];

		/*for(byte x : globalParams) {
			System.out.printf("%x ", x);
		}*/
		//System.out.printf("\n");

		reverb.set(globals.rvbparm);
		delay.set(globals.delparm);
		compr.set(globals.cprparm);

		lcfreq = Utils.sqr((globals.vlowcut + 1.0f) / 128.0f);
		hcfreq = Utils.sqr((globals.vhighcut + 1.0f) / 128.0f);
	}

	public void getPoly() {
		//TODO: Port V2Synth.getPoly()
	}

	public void getPgm() {
		//TODO: Port V2Synth.getPgm()
	}

	public V2Sound getpatch(int pgm) {
		byte[] voiceParams = new byte[59];
		byte[] channelParams = new byte[29];
		byte maxPoly;
		byte modulatorNum;

		//byte[] fuck = new byte[1234128];
		//pgm++;
		int offset = v2mBuffer.getInt(patchmap + (pgm * 4));
		//System.out.println("Offset: " + offset);

		v2mBuffer.rewind();
		v2mBuffer.position(patchmap + offset);
		v2mBuffer.get(voiceParams);
		v2mBuffer.get(channelParams);
		maxPoly = v2mBuffer.get();
		modulatorNum = v2mBuffer.get();

		V2Sound x = new V2Sound(modulatorNum);
		x.voice = voiceParams;
		x.chan = channelParams;
		x.maxpoly = maxPoly;
		x.modnum = modulatorNum;

		byte tmp = 0;

		for (int i = 0; i < modulatorNum; i++) {
			tmp = v2mBuffer.get();
			x.modmatrix[i].source = tmp;
			tmp = v2mBuffer.get();
			x.modmatrix[i].val = tmp;
			tmp = v2mBuffer.get();
			x.modmatrix[i].dest = tmp;
		}
		
		/*System.out.println(x.voice.length);
		for(byte e : x.voice) {
			System.out.print(Integer.toHexString(e & 0xff)+" ");
		}*/
		/*System.out.println("\n"+x.chan.length);
		for(byte e : x.chan) {
			System.out.print(Integer.toHexString(e & 0xff)+" ");
		}
		System.out.println("\n"+ x.maxpoly);
		System.out.println(x.modnum);*/
		//System.out.println(pgm);
		//System.out.println("Offset: " + Integer.toHexString(offset));
		//v2mBuffer.get(x.voice, patchmap , 59);

		return x;
	}

	float getmodsource(V2Voice voice, int chan, int source) {
		float mod_in = 0.0f;
		switch (source) {
			case 0:
				//System.out.print("MOD src vel ");
				mod_in = voice.velo;
				break;

			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				//System.out.print("MOD src ctl ");
				mod_in = chans[chan].ctl[source - 1];
				break;
			case 8:
			case 9:
				//System.out.print("MOD src EG ");
				mod_in = voice.env[source - 8].out;
				break;
			case 10:
			case 11:
				//System.out.print("MOD src LFO ");
				mod_in = voice.lfo[source - 10].out;
				break;
			default:
				//System.out.print("MOD src note ");
				mod_in = 2.0f * (voice.note - 48.0f);
				break;
		}

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
						print.printf("%d chan %d mod_in %f type %s\n", d_count, chan, mod_in, t);
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

		//System.out.printf("mod_in %f\n", mod_in);

		/* Debug writer END */
		//System.out.printf("%f %d\n", mod_in, source);
		return mod_in;
	}

	public void storeV2Values(int vind) {
		int chan = chanmap[vind];
		if (chan < 0) {
			return;
		}

		V2Sound patch = getpatch(chans[chan].pgm);

		syVV2 vpara = voicesv[vind];
		//float[] vparaf = vpara;
		V2Voice voice = voicesw[vind];

		//for(int i = 0; i < patch.voice.length; i++) {
		//vparaf[i] = (float)patch.voice[i];
		//}
		vpara.panning = patch.voice[0];
		vpara.transp = patch.voice[1];
		vpara.osc[0].mode = patch.voice[2];
		vpara.osc[0].ring = patch.voice[3];
		vpara.osc[0].pitch = patch.voice[4];
		vpara.osc[0].detune = patch.voice[5];
		vpara.osc[0].color = patch.voice[6];
		vpara.osc[0].gain = patch.voice[7];
		vpara.osc[1].mode = patch.voice[8];
		vpara.osc[1].ring = patch.voice[9];
		vpara.osc[1].pitch = patch.voice[10];
		vpara.osc[1].detune = patch.voice[11];
		vpara.osc[1].color = patch.voice[12];
		vpara.osc[1].gain = patch.voice[13];
		vpara.osc[2].mode = patch.voice[14];
		vpara.osc[2].ring = patch.voice[15];
		vpara.osc[2].pitch = patch.voice[16];
		vpara.osc[2].detune = patch.voice[17];
		vpara.osc[2].color = patch.voice[18];
		vpara.osc[2].gain = patch.voice[19];
		vpara.flt[0].mode = patch.voice[20];
		vpara.flt[0].cutoff = patch.voice[21];
		vpara.flt[0].reso = patch.voice[22];
		vpara.flt[1].mode = patch.voice[23];
		vpara.flt[1].cutoff = patch.voice[24];
		vpara.flt[1].reso = patch.voice[25];
		vpara.routing = patch.voice[26];
		vpara.fltbal = patch.voice[27];
		vpara.dist.mode = patch.voice[28];
		vpara.dist.ingain = patch.voice[29];
		vpara.dist.param1 = patch.voice[30];
		vpara.dist.param2 = patch.voice[31];
		vpara.env[0].ar = patch.voice[32];
		vpara.env[0].dr = patch.voice[33];
		vpara.env[0].sl = patch.voice[34];
		vpara.env[0].sr = patch.voice[35];
		vpara.env[0].rr = patch.voice[36];
		vpara.env[0].vol = patch.voice[37];
		vpara.env[1].ar = patch.voice[38];
		vpara.env[1].dr = patch.voice[39];
		vpara.env[1].sl = patch.voice[40];
		vpara.env[1].sr = patch.voice[41];
		vpara.env[1].rr = patch.voice[42];
		vpara.env[1].vol = patch.voice[43];
		vpara.lfo[0].mode = patch.voice[44];
		vpara.lfo[0].sync = patch.voice[45];
		vpara.lfo[0].egmode = patch.voice[46];
		vpara.lfo[0].rate = patch.voice[47];
		vpara.lfo[0].phase = patch.voice[48];
		vpara.lfo[0].pol = patch.voice[49];
		vpara.lfo[0].amp = patch.voice[50];
		vpara.lfo[1].mode = patch.voice[51];
		vpara.lfo[1].sync = patch.voice[52];
		vpara.lfo[1].egmode = patch.voice[53];
		vpara.lfo[1].rate = patch.voice[54];
		vpara.lfo[1].phase = patch.voice[55];
		vpara.lfo[1].pol = patch.voice[56];
		vpara.lfo[1].amp = patch.voice[57];
		vpara.oscsync = patch.voice[58];

		for (int i = 0; i < patch.modnum; i++) {
			V2Mod mod = patch.modmatrix[i];
			if (mod.dest >= patch.voice.length) {
				continue;
			}

			float scale = (mod.val - 64.0f) / 64.0f;
			//vpara = Utils.clamp(vpara + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
			//System.out.printf("Mod.src %d Mod.dst %d Mod.val %d Scale %f\n", mod.source, mod.dest, mod.val, scale);
			switch (mod.dest) {
				case 0:
					vpara.panning = Utils.clamp(vpara.panning + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 1:
					vpara.transp = Utils.clamp(vpara.transp + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 2:
					vpara.osc[0].mode = Utils.clamp(vpara.osc[0].mode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 3:
					vpara.osc[0].ring = Utils.clamp(vpara.osc[0].ring + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 4:
					vpara.osc[0].pitch = Utils.clamp(vpara.osc[0].pitch + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 5:
					vpara.osc[0].detune = Utils.clamp(vpara.osc[0].detune + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 6:
					vpara.osc[0].color = Utils.clamp(vpara.osc[0].color + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 7:
					vpara.osc[0].gain = Utils.clamp(vpara.osc[0].gain + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 8:
					vpara.osc[1].mode = Utils.clamp(vpara.osc[1].mode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 9:
					vpara.osc[1].ring = Utils.clamp(vpara.osc[1].ring + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 10:
					vpara.osc[1].pitch = Utils.clamp(vpara.osc[1].pitch + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 11:
					vpara.osc[1].detune = Utils.clamp(vpara.osc[1].detune + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 12:
					vpara.osc[1].color = Utils.clamp(vpara.osc[1].color + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 13:
					vpara.osc[1].gain = Utils.clamp(vpara.osc[1].gain + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 14:
					vpara.osc[2].mode = Utils.clamp(vpara.osc[2].mode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 15:
					vpara.osc[2].ring = Utils.clamp(vpara.osc[2].ring + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 16:
					vpara.osc[2].pitch = Utils.clamp(vpara.osc[2].pitch + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 17:
					vpara.osc[2].detune = Utils.clamp(vpara.osc[2].detune + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 18:
					vpara.osc[2].color = Utils.clamp(vpara.osc[2].color + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 19:
					vpara.osc[2].gain = Utils.clamp(vpara.osc[2].gain + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 20:
					vpara.flt[0].mode = Utils.clamp(vpara.flt[0].mode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 21:
					vpara.flt[0].cutoff = Utils.clamp(vpara.flt[0].cutoff + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 22:
					vpara.flt[0].reso = Utils.clamp(vpara.flt[0].reso + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 23:
					vpara.flt[1].mode = Utils.clamp(vpara.flt[1].mode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 24:
					vpara.flt[1].cutoff = Utils.clamp(vpara.flt[1].cutoff + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 25:
					vpara.flt[1].reso = Utils.clamp(vpara.flt[1].reso + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 26:
					vpara.routing = Utils.clamp(vpara.routing + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 27:
					vpara.fltbal = Utils.clamp(vpara.fltbal + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 28:
					vpara.dist.mode = Utils.clamp(vpara.dist.mode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 29:
					vpara.dist.ingain = Utils.clamp(vpara.dist.ingain + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 30:
					vpara.dist.param1 = Utils.clamp(vpara.dist.param1 + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 31:
					vpara.dist.param2 = Utils.clamp(vpara.dist.param2 + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 32:
					vpara.env[0].ar = Utils.clamp(vpara.env[0].ar + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 33:
					vpara.env[0].dr = Utils.clamp(vpara.env[0].dr + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 34:
					vpara.env[0].sl = Utils.clamp(vpara.env[0].sl + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 35:
					vpara.env[0].sr = Utils.clamp(vpara.env[0].sr + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 36:
					vpara.env[0].rr = Utils.clamp(vpara.env[0].rr + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 37:
					vpara.env[0].vol = Utils.clamp(vpara.env[0].vol + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 38:
					vpara.env[1].ar = Utils.clamp(vpara.env[1].ar + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 39:
					vpara.env[1].dr = Utils.clamp(vpara.env[1].dr + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 40:
					vpara.env[1].sl = Utils.clamp(vpara.env[1].sl + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 41:
					vpara.env[1].sr = Utils.clamp(vpara.env[1].sr + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 42:
					vpara.env[1].rr = Utils.clamp(vpara.env[1].rr + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 43:
					vpara.env[1].vol = Utils.clamp(vpara.env[1].vol + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 44:
					vpara.lfo[0].mode = Utils.clamp(vpara.lfo[0].mode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 45:
					vpara.lfo[0].sync = Utils.clamp(vpara.lfo[0].sync + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 46:
					vpara.lfo[0].egmode = Utils.clamp(vpara.lfo[0].egmode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 47:
					vpara.lfo[0].rate = Utils.clamp(vpara.lfo[0].rate + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 48:
					vpara.lfo[0].phase = Utils.clamp(vpara.lfo[0].phase + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 49:
					vpara.lfo[0].pol = Utils.clamp(vpara.lfo[0].pol + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 50:
					vpara.lfo[0].amp = Utils.clamp(vpara.lfo[0].amp + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 51:
					vpara.lfo[1].mode = Utils.clamp(vpara.lfo[1].mode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 52:
					vpara.lfo[1].sync = Utils.clamp(vpara.lfo[1].sync + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 53:
					vpara.lfo[1].egmode = Utils.clamp(vpara.lfo[1].egmode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 54:
					vpara.lfo[1].rate = Utils.clamp(vpara.lfo[1].rate + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 55:
					vpara.lfo[1].phase = Utils.clamp(vpara.lfo[1].phase + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 56:
					vpara.lfo[1].pol = Utils.clamp(vpara.lfo[1].pol + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 57:
					vpara.lfo[1].amp = Utils.clamp(vpara.lfo[1].amp + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 58:
					vpara.oscsync = Utils.clamp(vpara.oscsync + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
			}
		}

		voice.set(vpara);
	}

	public void storeChanValues(int chan) {

		V2Sound patch = getpatch(chans[chan].pgm);

		syVChan cpara = chansv[chan];
		//float[] cparaf = cpara;
		V2Chan cwork = chansw[chan];
		V2Voice voice = voicesw[voicemap[chan]];

		cpara.chanvol = patch.chan[0];
		cpara.auxarcv = patch.chan[1];
		cpara.auxbrcv = patch.chan[2];
		cpara.auxasnd = patch.chan[3];
		cpara.auxbsnd = patch.chan[4];
		cpara.aux1 = patch.chan[5];
		cpara.aux2 = patch.chan[6];
		cpara.fxroute = patch.chan[7];
		cpara.boost.amount = patch.chan[8];
		cpara.dist.mode = patch.chan[9];
		cpara.dist.ingain = patch.chan[10];
		cpara.dist.param1 = patch.chan[11];
		cpara.dist.param2 = patch.chan[12];
		cpara.chorus.amount = patch.chan[13];
		cpara.chorus.fb = patch.chan[14];
		cpara.chorus.llength = patch.chan[15];
		cpara.chorus.rlength = patch.chan[16];
		cpara.chorus.mrate = patch.chan[17];
		cpara.chorus.mdepth = patch.chan[18];
		cpara.chorus.mphase = patch.chan[19];
		cpara.comp.mode = patch.chan[20];
		cpara.comp.stereo = patch.chan[21];
		cpara.comp.autogain = patch.chan[22];
		cpara.comp.lookahead = patch.chan[23];
		cpara.comp.threshold = patch.chan[24];
		cpara.comp.ratio = patch.chan[25];
		cpara.comp.attack = patch.chan[26];
		cpara.comp.release = patch.chan[27];
		cpara.comp.outgain = patch.chan[28];

		//for(int i = 0; i < patch.chan.length; i++) {
		//cparaf[i] = (float)patch.chan[i];
		//}

		for (int i = 0; i < patch.modnum; i++) {
			V2Mod mod = patch.modmatrix[i];
			int dest = mod.dest - patch.voice.length;
			if (dest < 0 || dest >= patch.chan.length) {
				continue;
			}

			float scale = (mod.val - 64.0f) / 64.0f;
			//System.out.println(scale);
			//cparaf[dest] = Utils.clamp(cparaf[dest] + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
			//float p = scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
			//System.out.printf("Mod.src %d Mod.dst %d Mod.val %d Scale %f\n", mod.source, mod.dest, mod.val, scale);
			switch (dest) {
				case 0:
					cpara.chanvol = Utils.clamp(cpara.chanvol + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 1:
					cpara.auxarcv = Utils.clamp(cpara.auxarcv + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 2:
					cpara.auxbrcv = Utils.clamp(cpara.auxbrcv + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 3:
					cpara.auxasnd = Utils.clamp(cpara.auxasnd + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 4:
					cpara.auxbsnd = Utils.clamp(cpara.auxbsnd + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 5:
					cpara.aux1 = Utils.clamp(cpara.aux1 + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 6:
					cpara.aux2 = Utils.clamp(cpara.aux2 + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 7:
					cpara.fxroute = Utils.clamp(cpara.fxroute + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 8:
					cpara.boost.amount = Utils.clamp(cpara.boost.amount + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 9:
					cpara.dist.mode = Utils.clamp(cpara.dist.mode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 10:
					cpara.dist.ingain = Utils.clamp(cpara.dist.ingain + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 11:
					cpara.dist.param1 = Utils.clamp(cpara.dist.param1 + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 12:
					cpara.dist.param2 = Utils.clamp(cpara.dist.param2 + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 13:
					cpara.chorus.amount = Utils.clamp(cpara.chorus.amount + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 14:
					cpara.chorus.fb = Utils.clamp(cpara.chorus.fb + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 15:
					cpara.chorus.llength = Utils.clamp(cpara.chorus.llength + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 16:
					cpara.chorus.rlength = Utils.clamp(cpara.chorus.rlength + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 17:
					cpara.chorus.mrate = Utils.clamp(cpara.chorus.mrate + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 18:
					cpara.chorus.mdepth = Utils.clamp(cpara.chorus.mdepth + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 19:
					cpara.chorus.mphase = Utils.clamp(cpara.chorus.mphase + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 20:
					cpara.comp.mode = Utils.clamp(cpara.comp.mode + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 21:
					cpara.comp.stereo = Utils.clamp(cpara.comp.stereo + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 22:
					cpara.comp.autogain = Utils.clamp(cpara.comp.autogain + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 23:
					cpara.comp.lookahead = Utils.clamp(cpara.comp.lookahead + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 24:
					cpara.comp.threshold = Utils.clamp(cpara.comp.threshold + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 25:
					cpara.comp.ratio = Utils.clamp(cpara.comp.ratio + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 26:
					cpara.comp.attack = Utils.clamp(cpara.comp.attack + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 27:
					cpara.comp.release = Utils.clamp(cpara.comp.release + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
				case 28:
					cpara.comp.outgain = Utils.clamp(cpara.comp.outgain + scale * getmodsource(voice, chan, mod.source), 0.0f, 128.0f);
					break;
			}
		}

		cwork.set(cpara);
	}

	public void tick() {
		for (int i = 0; i < POLY; i++) {
			if (chanmap[i] < 0) {
				continue;
			}

			storeV2Values(i);
			voicesw[i].tick();

			if (voicesw[i].env[0].state == V2Env.OFF) {
				chanmap[i] = -1;
			}
		}

		for (int i = 0; i < CHANS; i++) {
			storeChanValues(i);
		}

		//ronanCBTick();
		tickd = instance.SRcFrameSize;
		renderFrame();
	}

	public void renderFrame() {

		int nsamples = instance.SRcFrameSize;

		// Clear output buffer
		//memset(instance.mixbuf, 0, nsamples)

		// clear aux buffers
		//memset(instance.aux1buf, 0, nsamples * sizeof(float));
		//memset(instance.aux2buf, 0, nsamples * sizeof(float));
		//memset(instance.auxabuf, 0, nsamples * sizeofe(SteroSample));
		//memset(instance.auxbbuf, 0, nsamples * sizeof(StereoSample));
		for (int i = 0; i < nsamples; i++) {
			instance.mixbuf[i][1] = 0.0f;
			instance.mixbuf[i][0] = 0.0f;
			instance.aux1buf[i] = 0.0f;
			instance.aux2buf[i] = 0.0f;
			instance.auxabuf[i][0] = 0.0f;
			instance.auxabuf[i][1] = 0.0f;
			instance.auxbbuf[i][0] = 0.0f;
			instance.auxbbuf[i][1] = 0.0f;
		}

		for (int chan = 0; chan < CHANS; chan++) {
			int voice = 0;
			while (voice < POLY && chanmap[voice] != chan) {
				voice++;
			}

			if (voice == POLY) {
				continue;
			}

			// clear channelbuffer
			// memset(instance.chanbuf, 0, nsamples * sizeof(StereoSample));

			for (int i = 0; i < nsamples; i++) {
				instance.chanbuf[i][0] = 0.0f;
				instance.chanbuf[i][1] = 0.0f;
			}

			for (; voice < POLY; voice++) {
				if (chanmap[voice] != chan) {
					continue;
				}

				voicesw[voice].render(instance.chanbuf, nsamples);
			}

			/*try {
				FileOutputStream fos = new FileOutputStream("test"+chan+".raw", true);
				DataOutputStream dos = new DataOutputStream(fos);

				for (int i = 0; i < 128; i++) {
					dos.writeFloat(instance.chanbuf[i][0]);
				}
				dos.close();

			} catch (IOException e) {

			}*/

			/*if (chan == CHANS - 1) {
				ronanCBProcess(&ronan, &instance.chanbuf[0].l, nsamples)
			}*/

			chansw[chan].process(nsamples);
		}

		float[][] mix = instance.mixbuf;

		/*try {
			FileOutputStream fos = new FileOutputStream("test.raw", true);
			DataOutputStream dos = new DataOutputStream(fos);

			for (int i = 0; i < nsamples; i++) {
				dos.writeFloat(mix[i][0]);
			}
			dos.close();

		} catch (IOException e) {

		}*/

		reverb.render(mix, nsamples);
		delay.renderAux2Main(mix, nsamples);
		dcf.renderStereo(mix, mix, nsamples);

		// low cut/high cut
		float lcf = lcfreq, hcf = hcfreq;
		for (int i = 0; i < nsamples; i++) {
			for (int ch = 0; ch < 2; ch++) {
				
				// lowcut
				float x = mix[i][ch] - lcbuf[ch];
				lcbuf[ch] += lcf * x;

				// highcut
				if (hcf != 1.0f) {
					hcbuf[ch] += hcf * (x - hcbuf[ch]);
					x = hcbuf[ch];
				}

				mix[i][ch] = x;
			}
		}

		compr.render(mix, nsamples);
		//System.out.println(mix);

		/*try {
			FileOutputStream fos = new FileOutputStream("testyy.raw", true);
			DataOutputStream dos = new DataOutputStream(fos);

			for (int i = 0; i < 128; i++) {
				dos.writeFloat(mix[i][0]);
				dos.writeFloat(mix[i][1]);
			}
			dos.close();

		} catch (IOException e) {

		}*/

	}

}
