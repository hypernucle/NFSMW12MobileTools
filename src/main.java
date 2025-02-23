import java.io.IOException;

public class main {

	private static String about = "NFS Most Wanted (2012, mobile) modding tools by Hypercycle, v0.10.1"
			+ "\nUsage examples:"
			+ "\n\t# Music Playlists editor (full support):"
			+ "\n\t\t'unpack playlists playlists.sb', 'repack playlists.sb.json'"
			+ "\n\t# Basic SBin file repacker (DATA objects where possible + HEX-edits if you know what to do)."
			+ "\n\t# Applicable & tested for Races, CarDesc, StringData, Pursuit, Achievements, Career (Garage Cars) configs:"
			+ "\n\t\t'unpack common event_01_race.prefabs.sb', 'repack event_01_race.prefabs.sb.json'"
			+ "\n\t# Texture repacker (only single-texture .sba with RGBA (A8R8G8B8) format):"
			+ "\n\t\t'unpack texture texture_car_model_year_diffuse_00.sba', 'repack texture_car_model_year_diffuse_00.sba.json'";
	
	public static void main(String[] args) throws IOException {
		SBin sbinTools = new SBin();
		SBin.startup();
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
			sbinTools.getFNVHash(args[1], args[2]);
			break;
		default: 
			displayHelp();
			break;
		}
	}
	
	private static void displayHelp() {
		System.out.println(about);
	}
	
}