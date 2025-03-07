package util;

public enum SBinBlockType {
	SBIN, ENUM, STRU, FIEL, OHDR, DATA, CHDR, CDAT, BULK, BARG;
	
	public static byte[] getBytes(SBinBlockType type) {
		return HEXUtils.stringToBytes(type.toString());
	}
}
