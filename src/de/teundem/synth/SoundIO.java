package de.teundem.synth;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import de.teundem.v2mplayer.V2Mplayer;

public class SoundIO {
	
	private SampleThread m_thread;
	
	public void dsInit() {
		m_thread = new SampleThread();
		m_thread.start();
	}

	class SampleThread extends Thread {

		final static public int SAMPLING_RATE = 44100;
		//final static public int SAMPLE_SIZE = 2; //Sample size in bytes
		//final static public double BUFFER_DURATION = 0.500; //About a 100ms buffer

		//final static public int BUFFER = (int) (BUFFER_DURATION * SAMPLING_RATE * SAMPLE_SIZE);
		final static public int BUFFER = 8192;
		
		SourceDataLine line;
		//public double fFreq; //Set from the pitch slider
		public boolean bExitThread = false;

		private int getLineSampleCount() {
			return line.getBufferSize() - line.available();
		}

		public void run() {
			//double fCyclePosition = 0;

			try {
				AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
				DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, BUFFER);

				if (!AudioSystem.isLineSupported(info))
					throw new LineUnavailableException();

				line = (SourceDataLine) AudioSystem.getLine(info);
				line.open(format);
				line.start();
			} catch (LineUnavailableException e) {
				System.out.println("Line of that type is not available");
				e.printStackTrace();
				System.exit(-1);
			}

			V2Mplayer player = new V2Mplayer();
			
			ByteBuffer cBuf = ByteBuffer.allocate(BUFFER * 2 * 2);
			float[][] fBuf = new float[BUFFER][2];
			int c = 0;
			while (bExitThread == false) {

				//double fCycleInc = fFreq / SAMPLING_RATE; //Fraction of cycle between samples

				cBuf.clear(); //Toss out samples from previous pass

				/*for (int i = 0; i < SINE_PACKET_SIZE / SAMPLE_SIZE; i++) {
					cBuf.putShort((short) (Short.MAX_VALUE * Math.sin(2 * Math.PI * fCyclePosition)));
					
					fCyclePosition += fCycleInc;
					if (fCyclePosition > 1)
						fCyclePosition -= 1;
				}*/
				
				player.Render(fBuf, BUFFER);
				
				/*FileOutputStream fos = null;
				DataOutputStream dos = null;
				try {
					fos = new FileOutputStream("testxx.raw", true);
					dos = new DataOutputStream(fos);
				} catch (IOException e){
					System.out.println("Error opening flie");
				}*/
				
				for(int i = 0; i < BUFFER; i++) {
					short t1 = (short)(Math.round(fBuf[i][0] * (Short.MAX_VALUE / 2)));
					short t2 = (short)(Math.round(fBuf[i][1] * (Short.MAX_VALUE / 2)));
					//if(c >7030 && c < 7050) { System.out.printf("%d %f %d\n",i ,fBuf[i][0], t);}
					
					//for (int j = 0; i < 128; i++) {
						/*try {
							dos.writeShort(t);
						} catch (IOException e) {
							e.printStackTrace();
						}*/
					//}
					
					/*if (t > 32767) System.out.println("clip pos");
					if (t < -32768) System.out.println("clip neg");*/

					cBuf.putShort(t1);
					cBuf.putShort(t2);
					//cBuf.putShort((short) (fBuf[i][1] * Short.MAX_VALUE / 2));
					//cBuf.putFloat(fBuf[i][0]);
					//cBuf.putFloat(fBuf[i][1]);
					//cBuf.put((byte)(fBuf[i][0]*255));
					//System.out.printf("%f ", fBuf[i][0]);
				}
				//System.out.println();
				
				line.write(cBuf.array(), 0, cBuf.position());

				/*try {
					dos.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}*/

				try {
					while (getLineSampleCount() > BUFFER * 2 * 2)
						Thread.sleep(5L); // Give UI a chance to run 
				} catch (InterruptedException e) { // We don't care about this
				}
			}

			line.drain();
			line.close();
		}

		public void exit() {
			System.out.println("Exit");
			bExitThread = true;
		}
	}
}
