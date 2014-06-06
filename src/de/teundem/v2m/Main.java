package de.teundem.v2m;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

	public static ByteBuffer v2mBuffer;

	public static void readV2M() {
		//System.out.println("Reading V2M into Buffer");
		//Path path = Paths.get("C:/JavaAudioWorkspace/SynthTest/bin/de/teundem/v2m/pzero_new.v2m");
		//Path path = Paths.get("de/teundem/v2mplayer/v2_zeitmaschine_new.v2m");
		//Path path = Paths.get("pzero_new.v2m");
		//Path path = Paths.get("v2_zeitmaschine_new.v2m");
		//Path path = Paths.get("trance.v2m");
		Path path = Paths.get("V2Modules/angeldevil_new.v2m");
		try {
			byte[] cache = Files.readAllBytes(path);
			v2mBuffer = ByteBuffer.wrap(cache);
			v2mBuffer.order(ByteOrder.LITTLE_ENDIAN);
			//v2mBuffer.asIntBuffer();
			//System.out.println(data);

		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println("Buffersize: " + v2mBuffer.capacity());
	}

	public static void main(String[] args) {

		readV2M();
		int test;
		for (int i = 0; i < 10; i++) {
			test = v2mBuffer.getInt(i * 4);
			//System.out.println(Integer.toHexString(test));
		}
		/*System.out.println("ready");
		try {
			int tmp = System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}

	/*public static void main(String[] args) {
		DataInputStream in = null;
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream("C:/JavaAudioWorkspace/SynthTest/bin/de/teundem/v2m/pzero_new.v2m")));
		} catch (Exception e) {
			e.printStackTrace();
		}
		byte x = 0;
		try {
			x = in.readByte();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(x);
		
	}*/
}
