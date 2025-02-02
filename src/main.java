import java.io.IOException;

public class main {

	private static String about = "NFS Most Wanted (2012, mobile) modding tools by Hypercycle, v0.5"
			+ "\nUsage examples:"
			+ "\n'unpack career career.prefabs.sb', 'repack career.prefabs.sb.json'"
			+ "\n'unpack stringdata nfsmw_android.sb', 'repack nfsmw_android.sb.json'"
			+ "\n'unpack common event_01_race.prefabs.sb', 'repack event_01_race.prefabs.sb.json'"
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
		default: 
			displayHelp();
			break;
		}
	}
	
	private static void displayHelp() {
		System.out.println(about);
	}
	
}