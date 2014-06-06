#line 1 "c:\\viruzii_latestorig\\v2mplayer.cpp"
#line 1 "c:\\viruzii_latestorig\\v2mplayer.h"
typedef int               sInt;
typedef unsigned int      sUInt;
typedef sInt              sBool;
typedef char              sChar;

typedef signed   char     sS8;
typedef signed   short    sS16;
typedef signed   long     sS32;
typedef signed   __int64  sS64;

typedef unsigned char     sU8;
typedef unsigned short    sU16;
typedef unsigned long     sU32;
typedef unsigned __int64  sU64;

typedef float             sF32;
typedef double            sF64;




template<class T> inline T sMin(const T a, const T b) { return (a<b)?a:b;  }
template<class T> inline T sMax(const T a, const T b) { return (a>b)?a:b;  }
template<class T> inline T sClamp(const T x, const T min, const T max) { return sMax(min,sMin(max,x)); }

#line 49 "c:\\viruzii_latestorig\\v2mplayer.h"
class V2MPlayer
{
public:
  void Init(sU32 a_tickspersec=1000) { m_tpc=a_tickspersec; m_base.valid=0; }
	sBool Open (const void *a_v2mptr, sU32 a_samplerate=44100);
	void  Close ();
	void  Play (sU32 a_time=0);
	void  Stop (sU32 a_fadetime=0);
  void Render (sF32 *a_buffer, sU32 a_len, sBool a_add=0);
	static void __stdcall RenderProxy(void *a_this, sF32 *a_buffer, sU32 a_len) 
	{ 
		reinterpret_cast<V2MPlayer*>(a_this)->Render(a_buffer,a_len);
	}
  sBool IsPlaying();

private: 
	struct V2MBase
	{
		sBool valid;
		const sU8   *patchmap;
		const sU8   *globals;
		sU32	timediv;
		sU32	timediv2;
		sU32	maxtime;
		const sU8   *gptr;
		sU32  gdnum;
		struct Channel {
			sU32	notenum;
			const sU8		*noteptr;
			sU32	pcnum;
			const sU8		*pcptr;
			sU32	pbnum;
			const sU8		*pbptr;
			struct CC {
				sU32	ccnum;
				const sU8		*ccptr;
			} ctl[7];
		} chan[16];
		const char  *speechdata;
		const char  *speechptrs[256];
	};
	
	struct PlayerState
	{
		enum { OFF, STOPPED, PLAYING, } state;
		sU32	time;
		sU32	nexttime;
		const sU8		*gptr;
		sU32	gnt;
		sU32	gnr;
		sU32  usecs;
		sU32  num;
		sU32  den;
		sU32	tpq;
		sU32  bar;
		sU32  beat;
		sU32  tick;
		struct Channel {
			const sU8		*noteptr;
			sU32  notenr;
			sU32	notent;
			sU8		lastnte;
			sU8   lastvel;
			const sU8   *pcptr;
			sU32  pcnr;
			sU32  pcnt;
			sU8		lastpc;
			const sU8		*pbptr;
			sU32  pbnr;
			sU32  pbnt;
			sU8		lastpb0;
			sU8		lastpb1;
			struct CC {
				const sU8		*ccptr;
				sU32  ccnt;
				sU32  ccnr;
				sU8   lastcc;
			} ctl[7];
		} chan[16];
		sU32 cursmpl;
		sU32 smpldelta;
		sU32 smplrem;
		sU32 tdif;
	};
  
  sU8         m_synth[3*1024*1024];   
	
	sU32        m_tpc;
	V2MBase			m_base;
	PlayerState m_state;
	sU32        m_samplerate;
	sS32				m_timeoffset;
	sU8         m_midibuf[4096];
	sF32        m_fadeval;
	sF32        m_fadedelta;

	sBool InitBase(const void *a_v2m);  
	void  Reset();                      
	void  Tick();                       

};

#line 255 "c:\\viruzii_latestorig\\v2mplayer.h"
#line 13 "c:\\viruzii_latestorig\\v2mplayer.cpp"
#line 1 "c:\\viruzii_latestorig\\libv2.h"

extern "C"
{
#line 19 "c:\\viruzii_latestorig\\libv2.h"
  
  typedef void (__stdcall DSIOCALLBACK)(void *parm, float *buf, unsigned long len);
  
  unsigned long __stdcall dsInit(DSIOCALLBACK *callback, void *parm, void *hWnd);
  
  void __stdcall dsClose();
  
  signed long __stdcall dsGetCurSmp();
  
  void __stdcall dsSetVolume(float vol);
  
  void __stdcall dsTick();
  
  void __stdcall dsLock();
  void __stdcall dsUnlock();
  
  unsigned int __stdcall synthGetSize();
  
  void __stdcall synthInit(void *pthis, const void *patchmap, int samplerate=44100);
  
  void __stdcall synthSetGlobals(void *pthis, const void *ptr);
  
  void __stdcall synthSetLyrics(void *pthis, const char **ptr);
  
  void __stdcall synthRender(void *pthis, void *buf, int smp, void *buf2=0, int add=0);
  
  void __stdcall synthProcessMIDI(void *pthis, const void *ptr);
  
  void __stdcall synthSetVUMode(void *pthis, int mode); 
  
  void __stdcall synthGetChannelVU(void *pthis, int ch, float *l, float *r); 
  
  void __stdcall synthGetMainVU(void *pthis, float *l, float *r);

}
#line 122 "c:\\viruzii_latestorig\\libv2.h"

#line 124 "c:\\viruzii_latestorig\\libv2.h"
#line 14 "c:\\viruzii_latestorig\\v2mplayer.cpp"

namespace
{
	void UpdateSampleDelta(sU32 nexttime, sU32 time, sU32 usecs, sU32 td2, sU32 *smplrem, sU32 *smpldelta)
	{
		
		__asm {
			mov eax, [nexttime]
			sub eax, [time]
			mov ebx, [usecs]
			mul ebx
			mov ebx, [td2]
			div ebx
			mov ecx, [smplrem]
			add [ecx], edx
			adc eax, 0
			mov ecx, [smpldelta]
			mov [ecx], eax
		}
	}
}

sBool V2MPlayer::InitBase(const void *a_v2m)
{
	const sU8 *d=(const sU8*)a_v2m;
	m_base.timediv=(*((sU32*)(d)));
	m_base.timediv2=10000*m_base.timediv;
	m_base.maxtime=*((sU32*)(d+4));
	m_base.gdnum=*((sU32*)(d+8));
	d+=12;
	m_base.gptr=d;
	d+=10*m_base.gdnum;
	for (sInt ch=0; ch<16; ch++)
	{
		V2MBase::Channel &c=m_base.chan[ch];
		c.notenum=*((sU32*)d);
		d+=4;
		if (c.notenum)
		{
			c.noteptr=d;
			d+=5*c.notenum;
			c.pcnum=*((sU32*)d);
			d+=4;
			c.pcptr=d;
			d+=4*c.pcnum;
			c.pbnum=*((sU32*)d);
			d+=4;
			c.pbptr=d;
			d+=5*c.pbnum;
			for (sInt cn=0; cn<7; cn++)
			{
				V2MBase::Channel::CC &cc=c.ctl[cn];
				cc.ccnum=*((sU32*)d);
				d+=4;
				cc.ccptr=d;
				d+=4*cc.ccnum;
			}						
		}		
	}
	sInt size=*((sU32*)d);
	if (size>16384 || size<0) return 0;
	d+=4;
	m_base.globals=d;
	d+=size;
	size=*((sU32*)d);
	if (size>1048576 || size<0) return 0;
	d+=4;
	m_base.patchmap=d;
	d+=size;

	sU32 spsize=*((sU32*)d);
	d+=4;
	if (!spsize || spsize>=8192)
	{
		for (sU32 i=0; i<256; i++)
			m_base.speechptrs[i]=" ";
	}
	else
	{
		m_base.speechdata=(const char *)d;
		d+=spsize;
		const sU32 *p32=(const sU32*)m_base.speechdata;
		sU32 n=*(p32++);
		for (sU32 i=0; i<n; i++)
		{
			m_base.speechptrs[i]=m_base.speechdata+*(p32++);
		}
	}

  return 1;
}

void V2MPlayer::Reset()
{
	m_state.time=0;
	m_state.nexttime=(sU32)-1;

	m_state.gptr=m_base.gptr;
	m_state.gnr=0;
	if ((m_state.gnr)<(m_base.gdnum)) { (m_state.gnt)=m_state.time+(((m_state.gptr))[0]+(((m_state.gptr))[(m_base.gdnum)]<<8)+(((m_state.gptr))[2*(m_base.gdnum)]<<16)); if ((m_state.gnt)<m_state.nexttime) m_state.nexttime=(m_state.gnt); };
	for (sInt ch=0; ch<16; ch++)
	{
		V2MBase::Channel &bc=m_base.chan[ch];
		PlayerState::Channel &sc=m_state.chan[ch];

		if (!bc.notenum) continue;
		sc.noteptr=bc.noteptr;
		sc.notenr=sc.lastnte=sc.lastvel=0;
		if ((sc.notenr)<(bc.notenum)) { (sc.notent)=m_state.time+(((sc.noteptr))[0]+(((sc.noteptr))[(bc.notenum)]<<8)+(((sc.noteptr))[2*(bc.notenum)]<<16)); if ((sc.notent)<m_state.nexttime) m_state.nexttime=(sc.notent); };
		sc.pcptr=bc.pcptr;
		sc.pcnr=sc.lastpc=0;
		if ((sc.pcnr)<(bc.pcnum)) { (sc.pcnt)=m_state.time+(((sc.pcptr))[0]+(((sc.pcptr))[(bc.pcnum)]<<8)+(((sc.pcptr))[2*(bc.pcnum)]<<16)); if ((sc.pcnt)<m_state.nexttime) m_state.nexttime=(sc.pcnt); };
		sc.pbptr=bc.pbptr;
		sc.pbnr=sc.lastpb0=sc.lastpb1=0;
		if ((sc.pbnr)<(bc.pcnum)) { (sc.pbnt)=m_state.time+(((sc.pbptr))[0]+(((sc.pbptr))[(bc.pcnum)]<<8)+(((sc.pbptr))[2*(bc.pcnum)]<<16)); if ((sc.pbnt)<m_state.nexttime) m_state.nexttime=(sc.pbnt); };
		for (sInt cn=0; cn<7; cn++)
		{
			V2MBase::Channel::CC &bcc=bc.ctl[cn];
			PlayerState::Channel::CC &scc=sc.ctl[cn];
			scc.ccptr=bcc.ccptr;
			scc.ccnr=scc.lastcc=0;
			if ((scc.ccnr)<(bcc.ccnum)) { (scc.ccnt)=m_state.time+(((scc.ccptr))[0]+(((scc.ccptr))[(bcc.ccnum)]<<8)+(((scc.ccptr))[2*(bcc.ccnum)]<<16)); if ((scc.ccnt)<m_state.nexttime) m_state.nexttime=(scc.ccnt); };
		}
	}
	m_state.usecs=5000*m_samplerate;
	m_state.num=4;
	m_state.den=4;
	m_state.tpq=8;
	m_state.bar=0;
	m_state.beat=0;
	m_state.tick=0;
	m_state.smplrem=0;

	if (m_samplerate)
	{
		synthInit(m_synth,(void*)m_base.patchmap,m_samplerate);
		synthSetGlobals(m_synth,(void*)m_base.globals);
		synthSetLyrics(m_synth,m_base.speechptrs);
	}
}

void V2MPlayer::Tick()
{
	if (m_state.state != PlayerState::PLAYING)
		return;

	m_state.tick+=m_state.nexttime-m_state.time;
	while (m_state.tick>=m_base.timediv)
	{
		m_state.tick-=m_base.timediv;
		m_state.beat++;
	}
	sU32 qpb=(m_state.num*4/m_state.den);
	while (m_state.beat>=qpb)
	{
		m_state.beat-=qpb;
		m_state.bar++;
	}

	m_state.time=m_state.nexttime;
	m_state.nexttime=(sU32)-1;
	sU8 *mptr=m_midibuf;
	sU32 laststat=-1;

	if (m_state.gnr<m_base.gdnum && m_state.time==m_state.gnt) 
	{
		m_state.usecs=(*(sU32 *)(m_state.gptr+3*m_base.gdnum+4*m_state.gnr))*(m_samplerate/100);
		m_state.num=m_state.gptr[7*m_base.gdnum+m_state.gnr];
		m_state.den=m_state.gptr[8*m_base.gdnum+m_state.gnr];
		m_state.tpq=m_state.gptr[9*m_base.gdnum+m_state.gnr];
		m_state.gnr++;
		if ((m_state.gnr)<(m_base.gdnum) && (((m_state.gptr+m_state.gnr))[0]+(((m_state.gptr+m_state.gnr))[(m_base.gdnum)]<<8)+(((m_state.gptr+m_state.gnr))[2*(m_base.gdnum)]<<16))) { (m_state.gnt)=m_state.time+(((m_state.gptr+m_state.gnr))[0]+(((m_state.gptr+m_state.gnr))[(m_base.gdnum)]<<8)+(((m_state.gptr+m_state.gnr))[2*(m_base.gdnum)]<<16)); };
	}
	if ((m_state.gnr)<(m_base.gdnum) && (m_state.gnt)<m_state.nexttime) m_state.nexttime=(m_state.gnt);;

	for (sInt ch=0; ch<16; ch++) 
	{
		V2MBase::Channel &bc=m_base.chan[ch];
		PlayerState::Channel &sc=m_state.chan[ch];
		if (!bc.notenum)
			continue;
		
		if (sc.pcnr<bc.pcnum && m_state.time==sc.pcnt)
		{
			{ sU8 bla=(0xc0|ch); if (laststat!=bla) { laststat=bla; *mptr++=(sU8)laststat; }};
			*mptr++=(sc.lastpc+=sc.pcptr[3*bc.pcnum]);
			sc.pcnr++;
			sc.pcptr++;
			if ((sc.pcnr)<(bc.pcnum) && (((sc.pcptr))[0]+(((sc.pcptr))[(bc.pcnum)]<<8)+(((sc.pcptr))[2*(bc.pcnum)]<<16))) { (sc.pcnt)=m_state.time+(((sc.pcptr))[0]+(((sc.pcptr))[(bc.pcnum)]<<8)+(((sc.pcptr))[2*(bc.pcnum)]<<16)); };
		}
		if ((sc.pcnr)<(bc.pcnum) && (sc.pcnt)<m_state.nexttime) m_state.nexttime=(sc.pcnt);;
		
 		for (sInt cn=0; cn<7; cn++)
		{
				V2MBase::Channel::CC &bcc=bc.ctl[cn];
				PlayerState::Channel::CC &scc=sc.ctl[cn];
				if (scc.ccnr<bcc.ccnum && m_state.time==scc.ccnt)
				{
					{ sU8 bla=(0xb0|ch); if (laststat!=bla) { laststat=bla; *mptr++=(sU8)laststat; }};
					*mptr++=cn+1;
					*mptr++=(scc.lastcc+=scc.ccptr[3*bcc.ccnum]);
					scc.ccnr++;
					scc.ccptr++;
					if ((scc.ccnr)<(bcc.ccnum) && (((scc.ccptr))[0]+(((scc.ccptr))[(bcc.ccnum)]<<8)+(((scc.ccptr))[2*(bcc.ccnum)]<<16))) { (scc.ccnt)=m_state.time+(((scc.ccptr))[0]+(((scc.ccptr))[(bcc.ccnum)]<<8)+(((scc.ccptr))[2*(bcc.ccnum)]<<16)); };
				}
				if ((scc.ccnr)<(bcc.ccnum) && (scc.ccnt)<m_state.nexttime) m_state.nexttime=(scc.ccnt);;
		}
		
		if (sc.pbnr<bc.pbnum && m_state.time==sc.pbnt)
		{
			{ sU8 bla=(0xe0|ch); if (laststat!=bla) { laststat=bla; *mptr++=(sU8)laststat; }};
			*mptr++=(sc.lastpb0+=sc.pbptr[3*bc.pcnum]);
			*mptr++=(sc.lastpb1+=sc.pbptr[4*bc.pcnum]);
			sc.pbnr++;
			sc.pbptr++;
			if ((sc.pbnr)<(bc.pbnum) && (((sc.pbptr))[0]+(((sc.pbptr))[(bc.pbnum)]<<8)+(((sc.pbptr))[2*(bc.pbnum)]<<16))) { (sc.pbnt)=m_state.time+(((sc.pbptr))[0]+(((sc.pbptr))[(bc.pbnum)]<<8)+(((sc.pbptr))[2*(bc.pbnum)]<<16)); };
		}
		if ((sc.pbnr)<(bc.pbnum) && (sc.pbnt)<m_state.nexttime) m_state.nexttime=(sc.pbnt);;
		
		while (sc.notenr<bc.notenum && m_state.time==sc.notent)
		{
			{ sU8 bla=(0x90|ch); if (laststat!=bla) { laststat=bla; *mptr++=(sU8)laststat; }};
			*mptr++=(sc.lastnte+=sc.noteptr[3*bc.notenum]);
			*mptr++=(sc.lastvel+=sc.noteptr[4*bc.notenum]);
			sc.notenr++;
			sc.noteptr++;
			if ((sc.notenr)<(bc.notenum) && (((sc.noteptr))[0]+(((sc.noteptr))[(bc.notenum)]<<8)+(((sc.noteptr))[2*(bc.notenum)]<<16))) { (sc.notent)=m_state.time+(((sc.noteptr))[0]+(((sc.noteptr))[(bc.notenum)]<<8)+(((sc.noteptr))[2*(bc.notenum)]<<16)); };
		}
		if ((sc.notenr)<(bc.notenum) && (sc.notent)<m_state.nexttime) m_state.nexttime=(sc.notent);;
	}

	*mptr++=0xfd;

	synthProcessMIDI(m_synth,m_midibuf);
	
	if (m_state.nexttime==(sU32)-1) m_state.state=PlayerState::STOPPED;
}

sBool V2MPlayer::Open(const void *a_v2mptr, sU32 a_samplerate)
{
	if (m_base.valid) Close();
	
	m_samplerate=a_samplerate;

	if (!InitBase(a_v2mptr)) return 0;

	Reset();

	return m_base.valid=1;
}

void V2MPlayer::Close()
{
	if (!m_base.valid) return;
	if (m_state.state!=PlayerState::OFF) Stop();

	m_base.valid=0;
}

void V2MPlayer::Play(sU32 a_time)
{
	if (!m_base.valid || !m_samplerate) return;

	Stop();
	Reset();

	m_base.valid=0;
	sU32 destsmpl, cursmpl=0;
	__asm
	{
		mov  ecx, this
		mov	 eax, [a_time]
		mov  ebx, [ecx + m_samplerate]
		imul ebx
		mov  ebx, [ecx + m_tpc]
		idiv ebx
		mov  [destsmpl], eax
	}

	m_state.state=PlayerState::PLAYING;
	m_state.smpldelta=0;
	m_state.smplrem=0;
	while ((cursmpl+m_state.smpldelta)<destsmpl && m_state.state==PlayerState::PLAYING)
	{
		cursmpl+=m_state.smpldelta;
		Tick();
		if (m_state.state==PlayerState::PLAYING)
		{
			UpdateSampleDelta(m_state.nexttime,m_state.time,m_state.usecs,m_base.timediv2,&m_state.smplrem,&m_state.smpldelta);
		}
		else
			m_state.smpldelta=-1;
	}
	m_state.smpldelta-=(destsmpl-cursmpl);
	m_timeoffset=cursmpl-m_state.cursmpl;
	m_fadeval=1.0f;
	m_fadedelta=0.0f;
	m_base.valid=1;
}

void V2MPlayer::Stop(sU32 a_fadetime)
{
	if (!m_base.valid) return;

	if (a_fadetime)
	{
		sU32 ftsmpls;
		__asm
		{
			mov  ecx, this
			mov	 eax, [a_fadetime]
			mov  ebx, [ecx + m_samplerate]
			imul ebx
			mov  ebx, [ecx + m_tpc]
			idiv ebx
			mov  [ftsmpls], eax
		}
		m_fadedelta=m_fadeval/ftsmpls;
	}
	else
		m_state.state=PlayerState::OFF;
}

void V2MPlayer::Render(sF32 *a_buffer, sU32 a_len, sBool a_add)
{
	if (!a_buffer) return;

	if (m_base.valid && m_state.state==PlayerState::PLAYING)
	{
		sU32 todo=a_len;
		while (todo)
		{
			sInt torender=(todo>m_state.smpldelta)?m_state.smpldelta:todo;
			if (torender)
			{
				synthRender(m_synth,a_buffer,torender,0,a_add);
				a_buffer+=2*torender;
				todo-=torender;
				m_state.smpldelta-=torender;
				m_state.cursmpl+=torender;
			}
			if (!m_state.smpldelta)
			{
				Tick();
				if (m_state.state==PlayerState::PLAYING)
					UpdateSampleDelta(m_state.nexttime,m_state.time,m_state.usecs,m_base.timediv2,&m_state.smplrem,&m_state.smpldelta);
				else
					m_state.smpldelta=-1;
			}
		}
	}
	else if (m_state.state==PlayerState::OFF || !m_base.valid)
	{
		if (!a_add)
		{
			  __asm {
				  mov edi, [a_buffer]
				  mov ecx, [a_len]
				  shl ecx, 1
				  xor eax, eax
				  rep stosd
			  }
		}
	}
	else
	{
		synthRender(m_synth,a_buffer,a_len,0,a_add);
		m_state.cursmpl+=a_len;
	}

	if (m_fadedelta)
	{
		for (sU32 i=0; i<a_len; i++)
		{
			a_buffer[2*i]*=m_fadeval;
			a_buffer[2*i+1]*=m_fadeval;
			m_fadeval-=m_fadedelta; if (m_fadeval<0) m_fadeval=0; 
		}
		if (!m_fadeval) Stop();
	}
}

sBool V2MPlayer::IsPlaying()
{
	return m_base.valid && m_state.state==PlayerState::PLAYING;
}
