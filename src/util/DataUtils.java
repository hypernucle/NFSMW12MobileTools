package util;

import java.util.List;

import util.DataClasses.SBinCDATEntry;

public class DataUtils {
	private DataUtils() {}
	
	public static byte[] processStringInCDAT(List<SBinCDATEntry> cdatList, String string) {
		for (SBinCDATEntry cdatEntry : cdatList) {
			if (cdatEntry.getString().contentEquals(string)) {
				return HEXUtils.decodeHexStr(cdatEntry.getChdrHexId());
			}
		}
		byte[] newCHDRId = HEXUtils.shortToBytes(cdatList.size());
		SBinCDATEntry newEntry = new SBinCDATEntry();
		newEntry.setString(string);
		newEntry.setChdrHexId(HEXUtils.hexToString(newCHDRId));
		cdatList.add(newEntry);
		return newCHDRId;
	}
}
