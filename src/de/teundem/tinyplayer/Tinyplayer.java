package de.teundem.tinyplayer;

import de.teundem.synth.SoundIO;
import de.teundem.v2mplayer.V2Mplayer;

public class Tinyplayer {

	public static V2Mplayer player = new V2Mplayer();
	public static SoundIO io = new SoundIO();
	
	public static void main(String[] args) {
		/*System.out.println("\nFarbrausch Tiny Music Player v0.dontcare TWO\n");
		System.out.println("Code and Synthesizer (W) 2000-2008 kb/Farbrausch\n");
		System.out.println("\n\nNow Playing: 'Patient Zero' by Melwyn & Little Bitchard\n");*/

		int theTune = 0;

		player.Init();
		player.Open(theTune);

		//dsInit(player.RenderProxy, &player, GetForegroundWindow());
		io.dsInit();
		
		player.Play();
		//player.Play(199000);
		//player.Render(1, 500_000);
		//int startTicks = GetTickCount();

		//System.out.println("\n\npress ESC to quit\n");

		//dsClose();
		//player.Close();
	}

}
