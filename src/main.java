import java.io.IOException;

import util.LaunchParameters;
import util.LogEntity;

public class main {

	private static final String ABOUT = "\tNFS Most Wanted (2012, mobile) modding tools by Hypercycle, v0.16"
			+ "\n\tUsage examples:"
			+ "\n\t# Basic SBin file repacker (DATA objects where possible + HEX-edits if you know what to do)."
			+ "\n\t# Applicable & tested for Races, CarDesc, StringData, Pursuit, Achievements, Career (Garage Cars), Car Configs, Model Prefabs, etc. configs:"
			+ "\n\t\t'unpack event_01_race.prefabs.sb'"
			+ "\n\t# UI related files (Flow), Playlists - add them into HCStructFileArray.json file:"
			+ "\n\t\t'unpack your_file.sb'"
			+ "\n\t# Texture repacker (formats: RGBA (A8R8G8B8), RGB (R8G8B8)), unpacker (format: ETC1 (ETC_RGB)):"
			+ "\n\t\t'unpack texture_car_model_year_diffuse_00.sba'"
			+ "\n\n\t# Repack .json file, any type:"
			+ "\n\t\t'repack your_file_name.json'"
			+ "\n\t# Unpack extra parameters, after the File name:"
			+ "\n\t\t'-disableMipmapUnpack', '-disableDATAObjectsUnpack'";
	
	public static void main(String[] args) throws IOException, InterruptedException {
		LogEntity.initLogConfig();
		SBin sbinTools = new SBin();
		M3GTools m3gTools = new M3GTools();
		
		if (args.length == 0) {
			displayHelp();
			return;
		}
		switch(args[0]) {
		case "unpack":
			SBin.startup(args);
			sbinTools.unpackSBin(args[1], true);
			break;
		case "repack":
			SBin.startup(args);
			sbinTools.repackSBin(args[1], true);
			break;
		case "hash":
			SBin.startup(args);
			sbinTools.getFNVHash(args[1]);
			break;
		case "check":
			SBin.startup(args);
			FileCheck.checkFiles(args[1], sbinTools);
			break;
		case "map":
			LaunchParameters.checkM3GLaunchParameters(args);
			m3gTools.mapM3G(args);
			break;
		case "rebuild":
			LaunchParameters.checkM3GLaunchParameters(args);
			m3gTools.buildFromJson(args[1]);
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