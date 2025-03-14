import java.io.IOException;

import util.LaunchParameters;

public class main {

	private static final String ABOUT = "\tNFS Most Wanted (2012, mobile) modding tools by Hypercycle, v0.14"
			+ "\n\tUsage examples:"
			+ "\n\t# Music Playlists editor (full support):"
			+ "\n\t\t'unpack playlists playlists.sb', 'repack playlists.sb.json'"
			+ "\n\t# Basic SBin file repacker (DATA objects where possible + HEX-edits if you know what to do)."
			+ "\n\t# Applicable & tested for Races, CarDesc, StringData, Pursuit, Achievements, Career (Garage Cars) configs:"
			+ "\n\t\t'unpack common event_01_race.prefabs.sb', 'repack event_01_race.prefabs.sb.json'"
			+ "\n\t# Texture repacker (formats: RGBA (A8R8G8B8), RGB (R8G8B8)), unpacker (format: ETC1 (ETC_RGB)):"
			+ "\n\t\t'unpack texture texture_car_model_year_diffuse_00.sba', 'repack texture_car_model_year_diffuse_00.sba.json'"
			+ "\n\t# Unpack extra parameters, after the File name:"
			+ "\n\t\t'-disableMipmapUnpack', '-disableDATAObjectsUnpack'";
	
	public static void main(String[] args) throws IOException, InterruptedException {
		SBin sbinTools = new SBin();
		SBin.startup();
		LaunchParameters.checkLaunchParameters(args);
		if (args.length == 0) {
			displayHelp();
			return;
		}
		switch(args[0]) {
		case "unpack":
			sbinTools.unpackSBin(args[1], args[2]);
			break;
		case "repack":
			sbinTools.repackSBin(args[1]);
			break;
		case "hash":
			sbinTools.getFNVHash(args[1]);
			break;
		default: 
			displayHelp();
			break;
		}
	}
	
	private static void displayHelp() {
		System.out.println(ABOUT);
	}
	
}