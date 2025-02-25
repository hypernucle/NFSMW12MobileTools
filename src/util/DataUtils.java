package util;

import java.util.ArrayList;
import java.util.List;

import util.DataClasses.SBinCDATEntry;
import util.DataClasses.SBinDataElement;
import util.DataClasses.SBinDataField;
import util.DataClasses.SBinJson;

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
	
	public static SBinDataElement getDataElementByStructName(SBinJson sbinJson, String structName) {
		for (SBinDataElement dataElement : sbinJson.getDataElements()) {
			if (dataElement.getStructName().contentEquals(structName)) {
				return dataElement;
			}
		}
		return null;
	}
	
	public static List<SBinDataElement> getAllDataElementsByStructName(SBinJson sbinJson, String structName) {
		List<SBinDataElement> elements = new ArrayList<>();
		for (SBinDataElement dataElement : sbinJson.getDataElements()) {
			if (dataElement.getStructName().contentEquals(structName)) {
				elements.add(dataElement);
			}
		}
		return elements;
	}
	
	public static SBinDataField getDataFieldByName(SBinDataElement dataElement, String name) {
		for (SBinDataField field : dataElement.getFields()) {
			if (field.getName().contentEquals(name)) {
				return field;
			}
		}
		return null;
	}
	
	public static SBinDataElement getDataElementFromValueId(SBinJson sbinJson, SBinDataElement dataElement, String fieldName) {
		SBinDataField lookForElement = DataUtils.getDataFieldByName(dataElement, fieldName);
		if (lookForElement != null) {
			return sbinJson.getDataElements().get(HEXUtils.strHexToInt(lookForElement.getValue()));
		}
		return null;
	}
}
