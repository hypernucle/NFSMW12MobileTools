import java.io.IOException;

public class main {

	private static String about = "NFS Most Wanted (2012, mobile) modding tools by Hypercycle, v0.7"
			+ "\nUsage examples:"
			+ "\n\t# Career Garage car list (full support):"
			+ "\n\t\t'unpack career career.prefabs.sb', 'repack career.prefabs.sb.json'"
			+ "\n\t# Text Localizations editor (full support):"
			+ "\n\t\t'unpack stringdata nfsmw_android.sb', 'repack nfsmw_android.sb.json'"
			+ "\n\t# Achievements editor (full support):"
			+ "\n\t\t'unpack achievements achievements.prefabs.sb', 'repack achievements.prefabs.sb.json'"
			+ "\n\t# Basic SBin file repacker (HEX-edits if you know what to do):"
			+ "\n\t\t'unpack common event_01_race.prefabs.sb', 'repack event_01_race.prefabs.sb.json'"
			+ "\n\t# Primitive texture replacer (!!! expects ONLY 1024x A8B8G8R8 .dds & .sba):"
			+ "\n\t\t'unpack texture texture_car_model_year_diffuse_00.sba', 'repack texture_car_model_year_diffuse_00.sba.json'";
	
	public static void main(String[] args) throws IOException {
		SBin sbinTools = new SBin();
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
		case "ohdr":
			sbinTools.calcOHDR(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
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