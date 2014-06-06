package de.teundem.v2mplayer;

import static de.teundem.v2m.Main.*;
import de.teundem.synth.Synth_core;

public class V2Mplayer extends IV2MPlayer {

	/*public void UpdateSampleDelta(int nexttime, int time, int usecs, int td2, int smplrem, int smpldelta) {
		
	}*/

	public void UpdateSampleDelta(long nexttime, long time, int usecs, int td2) {
		//nexttime = Math.abs(nexttime);
		//System.out.printf("nexttime %d time %d usecs %d td2 %d ", nexttime, time, usecs, td2);
		m_state.smpldelta = ((nexttime - time) * usecs) / td2;
		m_state.smplrem += ((nexttime - time) * usecs) % td2;
		//System.out.printf("smpldelta %d smplrem %d", m_state.smpldelta, m_state.smplrem);
		//System.out.printf("\n");
	}

	@Override
	public void Init() {
		this.Init(1000);
	}

	@Override
	public void Init(int a_tickspersec) {
		m_tpc = a_tickspersec;
		m_base.valid = false;
		m_state.state = PlayerState.State.OFF;
	}

	@Override
	public boolean Open(int ptra_v2mptr) {
		return Open(ptra_v2mptr, 44100);
	}

	@Override
	public boolean Open(int ptra_v2mptr, int a_samplerate) {
		if (m_base.valid)
			Close();
		readV2M();
		m_samplerate = a_samplerate;
		if (!InitBase(ptra_v2mptr))
			return false;
		Reset();
		return m_base.valid = true;
	}

	@Override
	public void Close() {
		if (!m_base.valid)
			return;
		if (m_state.state != PlayerState.State.OFF)
			Stop(0);

		m_base.valid = false;
	}

	public void Play() {
		this.Play(0);
	}

	@Override
	public void Play(int a_time) {
		/*if (!m_base.valid || (m_samplerate == 0))
			return;*/

		Stop(0);
		Reset();

		m_base.valid = false;
		long destsmpl, cursmpl = 0;
		
		// ASM
		destsmpl = a_time * (m_samplerate / m_tpc);
		System.out.println(destsmpl);		
		m_state.state = PlayerState.State.PLAYING;
		m_state.smpldelta = 0;
		m_state.smplrem = 0;

		while ((cursmpl + m_state.smpldelta) < destsmpl && m_state.state == PlayerState.State.PLAYING) {
			cursmpl += m_state.smpldelta;
			Tick();
			if (m_state.state == PlayerState.State.PLAYING) {
				//UpdateSampleDelta(m_state.nexttime, m_state.time, m_state.usecs, m_base.timediv2, m_state.smplrem, m_state.smpldelta);
				UpdateSampleDelta(m_state.nexttime, m_state.time, m_state.usecs, m_base.timediv2);
			} else {
				m_state.smpldelta = -1;
			}
		}

		m_state.smpldelta -= (destsmpl - cursmpl);
		m_timeoffset = cursmpl - m_state.cursmpl;
		m_fadeval = 1.0f;
		m_fadedelta = 0.0f;
		m_base.valid = true;
	}

	@Override
	public void Stop() {
		this.Stop(0);
	}

	@Override
	public void Stop(int a_fadetime) {
		if (!m_base.valid)
			return;

		if (a_fadetime > 0) {

		} else {
			m_state.state = PlayerState.State.OFF;
		}
	}

	@Override
	public void Render(float[][] ptra_buffer, int a_len) {
		this.Render(ptra_buffer, a_len, false);
	}

	@Override
	public void Render(float[][] ptra_buffer, int a_len, boolean a_add) {
		/*if (ptra_buffer == null)
			return;*/
		//System.out.println("Fuck");
		int ptra_bufferp = 0;
		
		if (m_base.valid && m_state.state == PlayerState.State.PLAYING) {
			
			long todo = a_len;
			
			while (todo != 0) {
				
				long torender = (todo > m_state.smpldelta) ? m_state.smpldelta : todo;
				
				if (torender != 0) {
					//Synth_core.synthRender(m_synth, ptra_buffer, torender, 0, a_add);
					Synth_core.synthRender(ptra_bufferp, ptra_buffer, torender, 0.0f, a_add);
					//ptra_buffer += 2 * torender;
					ptra_bufferp += torender;
					//System.out.printf("ptra_bufferp %d torender %d \n", ptra_bufferp, torender);
					todo -= torender;
					m_state.smpldelta -= torender;
					m_state.cursmpl += torender;
				}
				
				if (m_state.smpldelta == 0) {
					
					Tick();
					
					if (m_state.state == PlayerState.State.PLAYING) {
						UpdateSampleDelta(m_state.nexttime, m_state.time, m_state.usecs, m_base.timediv2);
					} else {
						m_state.smpldelta = -1;
					}
				}
				
			}
		} else if (m_state.state == PlayerState.State.OFF || !m_base.valid) {
			if (!a_add) {

			}
		} else {
			//Synth_core.synthRender(m_synth, ptra_buffer, a_len, 0.0f, a_add);
			Synth_core.synthRender(ptra_bufferp, ptra_buffer, a_len, 0.0f, a_add);
			m_state.cursmpl += a_len;
		}

		if (m_fadedelta != 0) {
			for (int i = 0; i < a_len; i++) {
				//ptra_buffer[2 * i] *= m_fadeval;
				//ptra_buffer[2 * i +1] *= m_fadeval;
				m_fadeval -= m_fadedelta;
				if (m_fadedelta < 0)
					m_fadeval = 0;
			}
			if (m_fadeval == 0)
				Stop();
		}
	}

	@Override
	public boolean IsPlaying() {
		return m_base.valid && m_state.state == PlayerState.State.PLAYING;
	}

	@Override
	public boolean InitBase(int ptra_v2m) {
		int d = 0;
		m_base.timediv = v2mBuffer.getInt(d); //System.out.println(m_base.timediv);
		m_base.timediv2 = 10000 * m_base.timediv; //System.out.println(m_base.timediv2);
		m_base.maxtime = v2mBuffer.getInt(d + 4); //System.out.println(m_base.maxtime);
		m_base.gdnum = v2mBuffer.getInt(d + 8); //System.out.println(m_base.gdnum);
		d += 12;
		m_base.ptrgptr = d; //System.out.println(m_base.ptrgptr);
		d += 10 * m_base.gdnum; //System.out.println("d:" + Integer.toHexString(0x0042c000 + d));
		//System.out.println("--------");
		for (int ch = 0; ch < 16; ch++) {
			//int ch=0;
			//V2MBase.Channel c = m_base.chan[ch];
			m_base.chan[ch].notenum = v2mBuffer.getInt(d);
			//System.out.println(c.notenum);
			d += 4;
			if (m_base.chan[ch].notenum > 0) {
				m_base.chan[ch].ptrnoteptr = d; //System.out.println(Integer.toHexString(d));
				d += 5 * m_base.chan[ch].notenum;
				m_base.chan[ch].pcnum = v2mBuffer.getInt(d); //System.out.println(c.pcnum);
				d += 4;
				m_base.chan[ch].ptrpcptr = d; //System.out.println(Integer.toHexString(0x0042c000+c.ptrpcptr));
				d += 4 * m_base.chan[ch].pcnum;
				m_base.chan[ch].pbnum = v2mBuffer.getInt(d); //System.out.println(c.pbnum);
				d += 4;
				m_base.chan[ch].ptrpbptr = d; //System.out.println(Integer.toHexString(0x0042c000+c.ptrpbptr));
				d += 5 * m_base.chan[ch].pbnum;
				for (int cn = 0; cn < 7; cn++) {
					//int cn = 0;
					//V2MBase.Channel.CC cc = c.ctl[cn];
					m_base.chan[ch].ctl[cn].ccnum = v2mBuffer.getInt(d); //System.out.println(cc.ccnum);
					d += 4;
					m_base.chan[ch].ctl[cn].ptrccptr = d; //System.out.println(Integer.toHexString(0x0042c000+cc.ptrccptr));
					d += 4 * m_base.chan[ch].ctl[cn].ccnum;
					//System.out.println("d:" + Integer.toHexString(0x0042c000+d));
				}
			}
		}
		//System.out.println("d:" + Integer.toHexString(0x0042c000+d));
		int size = v2mBuffer.getInt(d); //System.out.println(size);
		if (size > 16384 || size < 0)
			return false;
		d += 4;
		m_base.ptrglobals = d; //System.out.println("d:" + Integer.toHexString(0x0042c000+d));
		d += size;
		size = v2mBuffer.getInt(d); //System.out.println("d:" + Integer.toHexString(0x0042c000+d));
		if (size > 1048576 || size < 0)
			return false;
		d += 4;
		m_base.ptrpatchmap = d; //System.out.println("d:" + Integer.toHexString(0x0042c000+d));
		d += size;

		int spsize = v2mBuffer.getInt(d); //System.out.println("d:" + Integer.toHexString(0x0042c000+d));
		d += 4;
		if (spsize == 0 || spsize >= 8192) {
			/*for(int i = 0; i < 256; i++) {
				m_base.ptrspeechptrs[i] = " ";
			}*/
		} else {
			m_base.ptrspeechdata = d;
			d += spsize;
		}
		//System.out.println("InitBase: "+m_base.chan[0].notenum);
		return true;
	}

	//#define GETDELTA(p, w) ((p)[0]+((p)[w]<<8)+((p)[2*w]<<16))
	//#define UPDATENT(n, v, p, w) if ((n)<(w)) { (v)=m_state.time+GETDELTA((p), (w)); if ((v)<m_state.nexttime) m_state.nexttime=(v); }
	//#define UPDATENT2(n, v, p, w) if ((n)<(w) && GETDELTA((p), (w))) { (v)=m_state.time+GETDELTA((p), (w)); }
	//#define UPDATENT3(n, v, p, w) if ((n)<(w) && (v)<m_state.nexttime) m_state.nexttime=(v); 
	//#define PUTSTAT(s) { sU8 bla=(s); if (laststat!=bla) { laststat=bla; *mptr++=(sU8)laststat; }};

	/*public void GETDELTA(int p, int w) {
		((p)[0]+((p)[w]<<8)+((p)[2*w]<<16))
	}*/

	/*public void UPDATENT(int n, int v, int p, int w) {
		if ((n)<(w)) { (v)=m_state.time+GETDELTA((p), (w)); if ((v)<m_state.nexttime) m_state.nexttime=(v); }
	}*/

	@Override
	public void Reset() {
		//System.out.println("RESET");
		//System.out.println("InitBase: "+m_base.chan[0].notenum);
		m_state.time = 0;
		m_state.nexttime = 0x0fffffff; //-1;
		m_state.ptrgptr = m_base.ptrgptr;
		m_state.gnr = 0;
		/* UPDATENT(m_state.gnr, m_state.gnt, m_state.ptrgptr, m_base.gdnum); */
		if (m_state.gnr < m_base.gdnum) {
			//m_state.gnt=m_state.time+(m_state.ptrgptr[0]+(m_state.ptrgptr[m_base.gdnum]<<8)+(m_state.ptrgptr[2*m_base.gdnum]<<16));
			//if (m_state.gnt < m_state.nexttime) m_state.nexttime = m_state.gnt;
			int x = v2mBuffer.getInt(m_state.ptrgptr + 0) & 0xff;
			int y = (v2mBuffer.getInt(m_state.ptrgptr + m_base.gdnum) << 8) & 0xff00;
			int z = (v2mBuffer.getInt(m_state.ptrgptr + (2 * m_base.gdnum)) << 16) & 0xff0000;
			//System.out.printf("%x %x %x :", x, y, z);
			//m_state.gnt=m_state.time+(v2mBuffer.get(m_state.ptrgptr+0)+(v2mBuffer.get(m_state.ptrgptr+m_base.gdnum)<<8)+(v2mBuffer.get(m_state.ptrgptr+(2*m_base.gdnum))<<16));
			m_state.gnt = m_state.time + (x + y + z);
			if (m_state.gnt < m_state.nexttime) {
				m_state.nexttime = m_state.gnt;
			}
			//System.out.printf(" %x\n", m_state.gnt);
		}
		/* UPDATENT(m_state.gnr, m_state.gnt, m_state.ptrgptr, m_base.gdnum); */
		for (int ch = 0; ch < 16; ch++) {
			V2MBase.Channel bc = m_base.chan[ch];
			PlayerState.Channel sc = m_state.chan[ch];

			if (bc.notenum == 0) {
				continue; //if(bc.notenum == 0) continue;
			}
			sc.ptrnoteptr = bc.ptrnoteptr;
			sc.notenr = sc.lastnte = sc.lastvel = 0;
			//udatent
			/*if ((sc.notenr)<(bc.notenum)) {
				(sc.notent)=m_state.time+(((sc.ptrnoteptr))[0]+(((sc.ptrnoteptr))[(bc.notenum)]<<8)+(((sc.ptrnoteptr))[2*(bc.notenum)]<<16));
				if ((sc.notent)<m_state.nexttime) {
					m_state.nexttime=(sc.notent);
				}
			}*/
			if ((sc.notenr) < (bc.notenum)) {
				int x = v2mBuffer.get(sc.ptrnoteptr + 0) & 0xff;
				int y = (v2mBuffer.get(sc.ptrnoteptr + bc.notenum) << 8) & 0xff00;
				int z = (v2mBuffer.get(sc.ptrnoteptr + (2 * bc.notenum)) << 16) & 0xff0000;
				//System.out.printf("%x %x %x :", x, y, z);
				//(sc.notent)=m_state.time+(v2mBuffer.get(sc.ptrnoteptr+0)+(v2mBuffer.get(sc.ptrnoteptr+bc.notenum)<<8)+(v2mBuffer.get(sc.ptrnoteptr+(2*bc.notenum))<<16));
				(sc.notent) = m_state.time + (x + y + z);
				if ((sc.notent) < m_state.nexttime) {
					m_state.nexttime = (sc.notent);
				}
				//System.out.printf(" %x\n", sc.notent);
			}
			//updatent
			sc.ptrpcptr = bc.ptrpcptr;
			sc.pcnr = sc.lastpc = 0;
			//updatent
			/*if ((sc.pcnr)<(bc.pcnum)) {
				(sc.pcnt)=m_state.time+(((sc.ptrpcptr))[0]+(((sc.ptrpcptr))[(bc.pcnum)]<<8)+(((sc.ptrpcptr))[2*(bc.pcnum)]<<16));
				if ((sc.pcnt)<m_state.nexttime) {
					m_state.nexttime=(sc.pcnt);
				}
			}*/
			if ((sc.pcnr) < (bc.pcnum)) {
				int x = v2mBuffer.get(sc.ptrpcptr + 0) & 0xff;
				int y = (v2mBuffer.get(sc.ptrpcptr + bc.pcnum) << 8) & 0xff00;
				int z = (v2mBuffer.get(sc.ptrpcptr + (2 * bc.pcnum)) << 16) & 0xff0000;
				//System.out.printf("%x %x %x :", x, y, z);
				//(sc.pcnt)=m_state.time+(v2mBuffer.get(sc.ptrpcptr+0)+(v2mBuffer.get(sc.ptrpcptr+bc.pcnum)<<8)+(v2mBuffer.get(sc.ptrpcptr+(2*bc.pcnum))<<16));
				(sc.pcnt) = m_state.time + (x + y + z);
				if ((sc.pcnt) < m_state.nexttime) {
					m_state.nexttime = (sc.pcnt);
				}
				//System.out.printf(" %x\n", sc.pcnt);
			}
			//updatent
			sc.ptrpbptr = bc.ptrpbptr;
			sc.pbnr = sc.lastpb0 = sc.lastpb1 = 0;
			//updatent
			/*if ((sc.pbnr)<(bc.pcnum)) {
				(sc.pbnt)=m_state.time+(((sc.ptrpbptr))[0]+(((sc.ptrpbptr))[(bc.pcnum)]<<8)+(((sc.ptrpbptr))[2*(bc.pcnum)]<<16));
				if ((sc.pbnt)<m_state.nexttime) {
					m_state.nexttime=(sc.pbnt);
				}
			}*/
			if ((sc.pbnr) < (bc.pcnum)) {
				int x = v2mBuffer.get(sc.ptrpbptr + 0) & 0xff;
				int y = (v2mBuffer.get(sc.ptrpbptr + bc.pcnum) << 8) & 0xff00;
				int z = (v2mBuffer.get(sc.ptrpbptr + (2 * bc.pcnum)) << 16) & 0xff0000;
				//System.out.printf("%x %x %x :", x, y ,z);
				//(sc.pbnt)=m_state.time+(v2mBuffer.get(sc.ptrpbptr+0)+(v2mBuffer.get(sc.ptrpbptr+bc.pcnum)<<8)+(v2mBuffer.get(sc.ptrpbptr+(2*bc.pcnum))<<16));
				(sc.pbnt) = m_state.time + (x + y + z);
				if ((sc.pbnt) < m_state.nexttime) {
					m_state.nexttime = (sc.pbnt);
				}
				//System.out.printf(" %x\n", sc.pbnt);
			}
			//updatent
			for (int cn = 0; cn < 7; cn++) {
				V2MBase.Channel.CC bcc = bc.ctl[cn];
				PlayerState.Channel.CC scc = sc.ctl[cn];
				scc.ptrccptr = bcc.ptrccptr;
				scc.ccnr = scc.lastcc = 0;
				//updatent
				/*if ((scc.ccnr)<(bcc.ccnum)) {
					(scc.ccnt)=m_state.time+(((scc.ptrccptr))[0]+(((scc.ptrccptr))[(bcc.ccnum)]<<8)+(((scc.ptrccptr))[2*(bcc.ccnum)]<<16));
					if ((scc.ccnt)<m_state.nexttime) {
						m_state.nexttime=(scc.ccnt);
					}
				}*/
				if ((scc.ccnr) < (bcc.ccnum)) {
					int x = v2mBuffer.get(scc.ptrccptr + 0) & 0xff;
					int y = (v2mBuffer.get(scc.ptrccptr + bcc.ccnum) << 8) & 0xff00;
					int z = (v2mBuffer.get(scc.ptrccptr + (2 * bcc.ccnum)) << 16) & 0xff0000;
					//System.out.printf("%x %x %x :", x, y ,z);
					//(scc.ccnt)=m_state.time+(v2mBuffer.get(scc.ptrccptr+0)+(v2mBuffer.get(scc.ptrccptr+bcc.ccnum)<<8)+(v2mBuffer.get(scc.ptrccptr+(2*bcc.ccnum))<<16));
					(scc.ccnt) = m_state.time + (x + y + z);
					if ((scc.ccnt) < m_state.nexttime) {
						m_state.nexttime = (scc.ccnt);
					}
					//System.out.printf(" %x\n", scc.ccnt);
				}
				//updatent
			}
		}

		m_state.usecs = 5000 * m_samplerate;
		m_state.num = 4;
		m_state.den = 4;
		m_state.tpq = 8;
		m_state.bar = 0;
		m_state.beat = 0;
		m_state.tick = 0;
		m_state.smplrem = 0;

		if (m_samplerate > 0) {
			Synth_core.synthInit(m_synth, m_base.ptrpatchmap, m_samplerate);
			Synth_core.synthSetGlobals(m_base.ptrglobals);
			//synthSetLyrics();
		}
	}

	@Override
	public void Tick() {
		if (m_state.state != PlayerState.State.PLAYING) {
			return;
		}

		m_state.tick += m_state.nexttime - m_state.time;
		while (m_state.tick >= m_base.timediv) {
			m_state.tick -= m_base.timediv;
			m_state.beat++;
		}

		int qpb = (m_state.num * 4 / m_state.den);
		while (m_state.beat >= qpb) {
			m_state.beat -= qpb;
			m_state.bar++;
		}

		m_state.time = m_state.nexttime;
		m_state.nexttime = 0xfffffff;
		int mptr = 0;
		int lastStatusByte = -1;

		if (m_state.gnr < m_base.gdnum && m_state.time == m_state.gnt) {
			m_state.usecs = (v2mBuffer.getInt(m_state.ptrgptr + 3) * m_base.gdnum + 4 * m_state.gnr) * (m_samplerate / 100);
			m_state.num = v2mBuffer.get(m_state.ptrgptr + (7 * m_base.gdnum + m_state.gnr));
			m_state.den = v2mBuffer.get(m_state.ptrgptr + (8 * m_base.gdnum + m_state.gnr));
			m_state.tpq = v2mBuffer.get(m_state.ptrgptr + (9 * m_base.gdnum + m_state.gnr));
			m_state.gnr++;
			//UPDATENT2(m_state.gnr, m_state.gnt, m_state.gptr+m_state.gnr, m_base.gdnum);
			int p = m_state.ptrgptr;
			int x1 = v2mBuffer.get(p + m_state.gnr + 0) & 0xff;
			int y1 = (v2mBuffer.get(p + m_state.gnr + m_base.gdnum) << 8) & 0xff00;
			int z1 = (v2mBuffer.get(p + m_state.gnr + (2 * m_base.gdnum)) << 16) & 0xff0000;
			if ((m_state.gnr) < (m_base.gdnum) && (x1 + y1 + z1) != 0) {
				(m_state.gnt) = m_state.time + (x1 + y1 + z1);
			}
			//UPDATENT2(m_state.gnr, m_state.gnt, m_state.gptr+m_state.gnr, m_base.gdnum);
		}
		//UPDATENT3(m_state.gnr, m_state.gnt, m_state.gptr+m_state.gnr, m_base.gdnum);
		if ((m_state.gnr) < (m_base.gdnum) && (m_state.gnt) < m_state.nexttime) {
			m_state.nexttime = (m_state.gnt);
		}
		//UPDATENT3(m_state.gnr, m_state.gnt, m_state.gptr+m_state.gnr, m_base.gdnum);

		for (int ch = 0; ch < 16; ch++) {
			V2MBase.Channel bc = m_base.chan[ch];
			PlayerState.Channel sc = m_state.chan[ch];
			if (bc.notenum == 0) {
				continue;
			}
			/*
			 * Programm Change
			 */
			if (sc.pcnr < bc.pcnum && m_state.time == sc.pcnt) {
				//PUTSTAT(0xc0|ch)
				//{ byte bla=(byte) (0xc0|ch); if (laststat!=bla) { laststat=bla; m_midibuf[mptr++]=(byte)laststat; }};
				{
					short bla = (short) (0xc0 | ch);
					if (lastStatusByte != bla) {
						lastStatusByte = bla;
						m_midibuf[mptr++] = (short) lastStatusByte;
					}
				}
				;
				//PUTSTAT(0xc0|ch)
				m_midibuf[mptr++] = (sc.lastpc += v2mBuffer.get(sc.ptrpcptr + (3 * bc.pcnum)) & 0xff);
				sc.pcnr++;
				sc.ptrpcptr++;
				//UPDATENT2(sc.pcnr,sc.pcnt,sc.pcptr,bc.pcnum);
				int p = sc.ptrpcptr;
				int x1 = v2mBuffer.get(p + 0) & 0xff;
				int y1 = (v2mBuffer.get(p + bc.pcnum) << 8) & 0xff00;
				int z1 = (v2mBuffer.get(p + (2 * bc.pcnum)) << 16) & 0xff0000;
				if ((sc.pcnr) < (bc.pcnum) && (x1 + y1 + z1) != 0) {
					(sc.pcnt) = m_state.time + (x1 + y1 + z1);
				}
				//UPDATENT2(sc.pcnr,sc.pcnt,sc.pcptr,bc.pcnum);
			}

			/*
			 * Control Changes
			 */
			for (int midiChannel = 0; midiChannel < 7; midiChannel++) {
				V2MBase.Channel.CC bcc = bc.ctl[midiChannel];
				PlayerState.Channel.CC scc = sc.ctl[midiChannel];

				if (scc.ccnr < bcc.ccnum && m_state.time == scc.ccnt) {
					//PUTSTAT(0xb0|ch)
					//{ byte bla=(byte)(0xb0|ch); if (laststat!=bla) { laststat=bla; m_midibuf[mptr++]=(byte)laststat; }};
					{
						short statusByte = (short) (0xb0 | ch);
						if (lastStatusByte != statusByte) {
							lastStatusByte = statusByte;
							m_midibuf[mptr++] = (short) lastStatusByte;
						}
					}
					;
					//PUTSTAT(0xb0|ch)
					//m_midibuf[mptr++] = (byte)(cn + 1);
					m_midibuf[mptr++] = (short) (midiChannel + 1);
					m_midibuf[mptr++] = (scc.lastcc += v2mBuffer.get(scc.ptrccptr + (3 * bcc.ccnum)) & 0xff);
					scc.ccnr++;
					scc.ptrccptr++;
					//UPDATENT2(scc.ccnr,scc.ccnt,scc.ccptr,bcc.ccnum);
					int p = scc.ptrccptr;
					int x1 = v2mBuffer.get(p + 0) & 0xff;
					int y1 = (v2mBuffer.get(p + bcc.ccnum) << 8) & 0xff00;
					int z1 = (v2mBuffer.get(p + (2 * bcc.ccnum)) << 16) & 0xff0000;
					if ((scc.ccnr) < (bcc.ccnum) && (x1 + y1 + z1) != 0) {
						(scc.ccnt) = m_state.time + (x1 + y1 + z1);
					}
					//UPDATENT2(scc.ccnr,scc.ccnt,scc.ccptr,bcc.ccnum);
				}
				//UPDATENT3(scc.ccnr,scc.ccnt,scc.ccptr,bcc.ccnum);
				if ((scc.ccnr) < (bcc.ccnum) && (scc.ccnt) < m_state.nexttime)
					m_state.nexttime = (scc.ccnt);
				//UPDATENT3(scc.ccnr,scc.ccnt,scc.ccptr,bcc.ccnum);
			}

			/*
			 * Pitch Bend
			 */
			if (sc.pbnr < bc.pbnum && m_state.time == sc.pbnt) {
				//PUTSTAT(0xe0|ch)
				//{ byte bla=(byte)(0xe0|ch); if (laststat!=bla) { laststat=bla; m_midibuf[mptr++]=(byte)laststat; }};
				{
					short bla = (short) (0xe0 | ch);
					if (lastStatusByte != bla) {
						lastStatusByte = bla;
						m_midibuf[mptr++] = (short) lastStatusByte;
					}
				}
				//PUTSTAT(0xe0|ch)
				m_midibuf[mptr++] = (sc.lastpb0 += v2mBuffer.get(sc.ptrpbptr + (3 * bc.pcnum)));
				m_midibuf[mptr++] = (sc.lastpb1 += v2mBuffer.get(sc.ptrpbptr + (4 * bc.pcnum)));
				sc.pbnr++;
				sc.ptrpbptr++;
				//UPDATENT2(sc.pbnr,sc.pbnt,sc.pbptr,bc.pbnum);
				int p = sc.ptrpbptr;
				int x1 = v2mBuffer.get(p + 0) & 0xff;
				int y1 = (v2mBuffer.get(p + bc.pbnum) << 8) & 0xff00;
				int z1 = (v2mBuffer.get(sc.ptrpbptr + (2 * bc.pbnum)) << 16) & 0xff0000;
				if ((sc.pbnr) < (bc.pbnum) && (x1 + y1 + z1) != 0) {
					(sc.pbnt) = m_state.time + (x1 + y1 + z1);
				}
				//UPDATENT2(sc.pbnr,sc.pbnt,sc.pbptr,bc.pbnum);
			}
			//UPDATENT3(sc.pbnr,sc.pbnt,sc.pbptr,bc.pbnum);
			if ((sc.pbnr) < (bc.pbnum) && (sc.pbnt) < m_state.nexttime)
				m_state.nexttime = (sc.pbnt);
			//UPDATENT3(sc.pbnr,sc.pbnt,sc.pbptr,bc.pbnum);

			/*
			 * Notes
			 */
			while (sc.notenr < bc.notenum && m_state.time == sc.notent) {
				//PUTSTAT(0x90|ch)
				//{ byte bla=(byte)(0x90|ch); if (laststat!=bla) { laststat=bla; m_midibuf[mptr++]=(byte)laststat; }};
				{
					short bla = (short) (0x90 | ch);
					if (lastStatusByte != bla) {
						lastStatusByte = bla;
						m_midibuf[mptr++] = (short) lastStatusByte;
					}
				}
				;
				//PUTSTAT(0x90|ch)
				m_midibuf[mptr++] = (sc.lastnte += v2mBuffer.get(sc.ptrnoteptr + (3 * bc.notenum)));
				m_midibuf[mptr++] = (sc.lastvel += v2mBuffer.get(sc.ptrnoteptr + (4 * bc.notenum)));
				sc.notenr++;
				sc.ptrnoteptr++;
				//UPDATENT2(sc.notenr,sc.notent,sc.noteptr,bc.notenum);
				int p = sc.ptrnoteptr;
				int x1 = v2mBuffer.get(p + 0) & 0xff;
				int y1 = (v2mBuffer.get(p + bc.notenum) << 8) & 0xff00;
				int z1 = (v2mBuffer.get(p + (2 * bc.notenum)) << 16) & 0xff0000;
				if ((sc.notenr) < (bc.notenum) && (x1 + y1 + z1) != 0) {
					(sc.notent) = m_state.time + (x1 + y1 + z1);
				}
				//UPDATENT2(sc.notenr,sc.notent,sc.noteptr,bc.notenum);
			}
			//UPDATENT3(sc.notenr,sc.notent,sc.noteptr,bc.notenum);
			if ((sc.notenr) < (bc.notenum) && (sc.notent) < m_state.nexttime)
				m_state.nexttime = (sc.notent);
			//UPDATENT3(sc.notenr,sc.notent,sc.noteptr,bc.notenum);
		}

		m_midibuf[mptr++] = (short) (0xfd);

		Synth_core.synthProcessMIDI(m_synth, m_midibuf);

		if (m_state.nexttime == -1)
			m_state.state = PlayerState.State.STOPPED;
	}

	/*public static void main(String[] args) {
		readV2M();
		V2Mplayer player = new V2Mplayer();
		player.Init(1000);
		player.Open(0, 44100);
		boolean b = player.InitBase(0);
		if(b == true) System.out.println("InitBase Success");
		player.Reset();
		player.Tick();
	}*/
}
