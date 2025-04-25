package util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LaunchParameters {
	private LaunchParameters() {}
	
	private static final Logger jl = Logger.getLogger(LogEntity.class.getSimpleName());
	
	private static final String DISABLE_MIPMAP_UNPACK_STR = "-disableMipmapUnpack";
	private static boolean disableMipmapUnpack = false;
	//
	private static final String DISABLE_DATA_OBJECTS_UNPACK_STR = "-disableDATAObjectsUnpack";
	private static boolean disableDATAObjectsUnpack = false;
	
	public static void checkLaunchParameters(String[] args) {
		if (!args[0].contentEquals("unpack")) {return;}
		
		SBJson.get().setSBinType(getSBinTypeByFileName(args[1]));
		
		if (args.length != 3) {return;}
		
		switch(args[2]) {
		case DISABLE_DATA_OBJECTS_UNPACK_STR:
			jl.log(Level.INFO, "Launch Parameter: DATA objects unpacking disabled. \nAll DATA objects will be properly"
					+ "splitted according by OHDR table, but without any Object parsing. 1-to-1 repacking still should be possible.");
			disableDATAObjectsUnpack();
			break;
		case DISABLE_MIPMAP_UNPACK_STR:
			jl.log(Level.INFO, "Launch Parameter: Mipmaps unpacking disabled. \nNote that the DATA objects "
					+ "could still contain Mipmaps information, so you must generate all Mipmaps manually, "
					+ "or edit .sba Json accordingly.");
			disableMipmapUnpack();
			break;
		default: 
			jl.log(Level.INFO, "Wrong Launch Parameter: {0}, ignored.", args[3]);
			break;
		}
	}
	
	public static SBinType getSBinTypeByFileName(String fileName) {
		if (SBJson.getHCStructFileArray().contains(fileName)) {
			return SBinType.HCSTRUCTS_COMMON;
		} 
		
		switch(fileName) {
		case "layouts.sb":
			return SBinType.LAYOUTS;
		case "fonts.sb":
			return SBinType.FONTS;
		case "tweaks.sb": case "tweaks_ipad.sb":
			return SBinType.TWEAKS;
		case "nfsmw_android.sb": case "nfs_mw_ios.sb":
			return SBinType.STRING_DATA;
		case "fake_names.sb":
			return SBinType.FAKE_NAMES;
		case "regions.sb":
			return SBinType.REGIONS;
		case "locales.sb":
			return SBinType.LOCALES;
		case "nfstr_save.sb":
			return SBinType.SAVES;
		default: 
			if (fileName.endsWith(".config")) {
				return SBinType.CAR_CONFIG;
			} else if (fileName.endsWith(".sba")) {
				return SBinType.TEXTURE;
			} else if (fileName.startsWith("roadblock_")) {
				return SBinType.ROADBLOCK_LEVEL;
			} else if (fileName.contains("skydome") && fileName.contains("prefabs.sb")) {
				return SBinType.SKYDOME;
			} 
			return SBinType.COMMON;
		}
	}
	
	public static void disableDATAObjectsUnpack() {
		disableDATAObjectsUnpack = true;
	}
	public static boolean isDATAObjectsUnpackDisabled() {
		return disableDATAObjectsUnpack;
	}
	
	public static void disableMipmapUnpack() {
		disableMipmapUnpack = true;
	}
	public static boolean isMipmapUnpackDisabled() {
		return disableMipmapUnpack;
	}

}
