package util;

public class LaunchParameters {
	private LaunchParameters() {}
	
	private static final String DISABLE_MIPMAP_UNPACK_STR = "-disableMipmapUnpack";
	private static boolean disableMipmapUnpack = false;
	//
	private static final String DISABLE_DATA_OBJECTS_UNPACK_STR = "-disableDATAObjectsUnpack";
	private static boolean disableDATAObjectsUnpack = false;
	
	public static void checkLaunchParameters(String[] args) {
		if (args.length != 4 || !args[0].contentEquals("unpack")) {return;}
		
		switch(args[3]) {
		case DISABLE_DATA_OBJECTS_UNPACK_STR:
			System.out.println("### Launch Parameter: DATA objects unpacking disabled. \nAll DATA objects will be properly "
					+ "splitted according by OHDR table, but without any Object parsing. 1-to-1 repacking still should be possible.");
			disableDATAObjectsUnpack();
			break;
		case DISABLE_MIPMAP_UNPACK_STR:
			System.out.println("### Launch Parameter: Mipmaps unpacking disabled. \nNote that the DATA objects "
					+ "could still contain Mipmaps information, so you must generate all Mipmaps manually, "
					+ "or edit .sba Json accordingly.");
			disableMipmapUnpack();
			break;
		default: 
			System.out.println("### Wrong Launch Parameter: " + args[3] + ", ignored.");
			break;
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
