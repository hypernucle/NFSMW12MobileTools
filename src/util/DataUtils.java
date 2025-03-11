package util;

import java.util.ArrayList;
import java.util.List;

import util.DataClasses.SBinCDATEntry;
import util.DataClasses.SBinDataElement;
import util.DataClasses.SBinDataField;
import util.DataClasses.SBinField;
import util.DataClasses.SBinJson;
import util.DataClasses.SBinStruct;

public class DataUtils {
	private DataUtils() {}
	
	public static byte[] processStringInCDAT(SBinJson sbinJson, String string) {
		for (SBinCDATEntry cdatEntry : sbinJson.getCDATStrings()) {
			if (cdatEntry.getString().contentEquals(string)) {
				return HEXUtils.decodeHexStr(cdatEntry.getChdrHexId());
			}
		}
		byte[] newCHDRId = HEXUtils.setDataEntryHexIdBytes(sbinJson.getCDATStrings().size(), sbinJson.isDataLongElementIds());
		SBinCDATEntry newEntry = new SBinCDATEntry();
		newEntry.setString(string);
		newEntry.setChdrHexId(HEXUtils.hexToString(newCHDRId));
		sbinJson.getCDATStrings().add(newEntry);
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
	
	public static SBinStruct getStructByName(SBinJson sbinJson, String name) {
		for (SBinStruct struct : sbinJson.getStructs()) {
			if (struct.getName().contentEquals(name)) {
				return struct;
			}
		}
		return null;
	}
	
	public static boolean checkForOverrideField(SBinJson sbinJson, SBinFieldType type) {
		for (SBinField field : sbinJson.getEmptyFields()) {
			if (field.getFieldTypeEnum().equals(type)) {
				return true;
			}
		}
		return false;
	}
}
