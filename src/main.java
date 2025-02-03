import java.io.IOException;

public class main {

	private static String about = "NFS Most Wanted (2012, mobile) modding tools by Hypercycle, v0.5"
			+ "\nUsage examples:"
			+ "\n# Career Garage car list (full support):"
			+ "\n'unpack career career.prefabs.sb', 'repack career.prefabs.sb.json'"
			+ "\n# Text Localizations editor (full support):"
			+ "\n'unpack stringdata nfsmw_android.sb', 'repack nfsmw_android.sb.json'"
			+ "\n# Basic SBin file repacker (HEX-edits if you know what to do):"
			+ "\n'unpack common event_01_race.prefabs.sb', 'repack event_01_race.prefabs.sb.json'"
			+ "\n# Primitive texture replacer (!!! expects ONLY 1024x A8B8G8R8 .dds & .sba):"
			+ "\n'unpack texture texture_car_model_year_diffuse_00.sba', 'repack texture_car_model_year_diffuse_00.sba.json'";
	
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