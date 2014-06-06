package de.teundem.synth;

public class syVChan {
	float chanvol;
	float auxarcv;
	float auxbrcv;
	float auxasnd;
	float auxbsnd;
	float aux1;
	float aux2;
	float fxroute;

	syVBoost boost = new syVBoost();
	syVDist dist = new syVDist();
	syVModDel chorus = new syVModDel();
	syVComp comp = new syVComp();
}
