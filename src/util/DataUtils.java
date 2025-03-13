package util;

import java.util.ArrayList;
import java.util.List;

import util.DataClasses.SBinCDATEntry;
import util.DataClasses.SBinDataElement;
import util.DataClasses.SBinDataField;
import util.DataClasses.SBinField;
import util.DataClasses.SBinStruct;

public class DataUtils {
	private DataUtils() {}
	
	public static byte[] processStringInCDAT(String string) {
		for (SBinCDATEntry cdatEntry : SBJson.get().getCDATStrings()) {
			if (cdatEntry.getString().contentEquals(string)) {
				return HEXUtils.decodeHexStr(cdatEntry.getChdrHexId());
			}
		}
		byte[] newCHDRId = HEXUtils.setDataEntryHexIdBytes(SBJson.get().getCDATStrings().size(), SBJson.get().isDataLongElementIds());
		SBinCDATEntry newEntry = new SBinCDATEntry();
		newEntry.setString(string);
		newEntry.setChdrHexId(HEXUtils.hexToString(newCHDRId));
		SBJson.get().getCDATStrings().add(newEntry);
		return newCHDRId;
	}
	
	public static SBinDataElement getDataElementByStructName(String structName) {
		for (SBinDataElement dataElement : SBJson.get().getDataElements()) {
			if (dataElement.getStructName().contentEquals(structName)) {
				return dataElement;
			}
		}
		return null;
	}
	
	public static List<SBinDataElement> getAllDataElementsByStructName(String structName) {
		List<SBinDataElement> elements = new ArrayList<>();
		for (SBinDataElement dataElement : SBJson.get().getDataElements()) {
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
	
	public static SBinDataElement getDataElementFromValueId(SBinDataElement dataElement, String fieldName) {
		SBinDataField lookForElement = DataUtils.getDataFieldByName(dataElement, fieldName);
		if (lookForElement != null) {
			return SBJson.get().getDataElements().get(HEXUtils.strHexToInt(lookForElement.getValue()));
		}
		return null;
	}
	
	public static SBinStruct getStructByName(String name) {
		for (SBinStruct struct : SBJson.get().getStructs()) {
			if (struct.getName().contentEquals(name)) {
				return struct;
			}
		}
		return null;
	}
	
}
