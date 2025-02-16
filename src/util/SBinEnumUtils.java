package util;

import util.DataClasses.SBinDataField;
import util.DataClasses.SBinJson;

public class SBinEnumUtils {
	
	//
	// SBinFieldType
	//
	
	public static int getFieldStandardSize(SBinFieldType type) {
		int size = 0x0;
		switch(type) {
		case INT8:
			size = 0x1;
			break;
		case INT32: case FLOAT: case DATA_ID_REF: case DATA_ID_MAP: case INT32_0X12: case INT32_0X16:
			size = 0x4;
			break;
		case BOOLEAN: case CHDR_ID_REF:
			size = 0x2;
			break;
		default: break;
		}
		return size;
	}
	
	public static String formatFieldValueUnpack(SBinFieldType type, SBinDataField dataField, int fieldRealSize, byte[] valueHex, SBinJson sbinJson) {
		String strValue;
		if (getFieldStandardSize(type) != fieldRealSize) {
			dataField.setForcedHexValue(true);
			return getDefaultHEXString(valueHex);
		}
		switch(type) {
		case INT32: case INT32_0X12: case INT32_0X16:
			strValue = String.valueOf(HEXUtils.byteArrayToInt(valueHex));
			break;
		case FLOAT:
			strValue = String.valueOf(HEXUtils.bytesToFloat(valueHex));
			break;
		case BOOLEAN: 
			int valueInt = HEXUtils.twoLEByteArrayToInt(valueHex);
			if (valueInt < 2) { // Precaution in case of unknown value type
				strValue = Boolean.toString(valueInt == 1);
			} else {
				strValue = String.valueOf(valueInt);
			}
			break;
		case CHDR_ID_REF: 
			strValue = sbinJson.getCDATStrings().get(HEXUtils.twoLEByteArrayToInt(valueHex)).getString();
			break;
		case INT8: case DATA_ID_REF: case DATA_ID_MAP: default: 
			// INT8: Primarily used for HEX colors, left as it is
			// DATA_ID: simpler to provide HEX code and compare/find with other DATA info
			strValue = getDefaultHEXString(valueHex);
			break;
		}
		return strValue;
	}
	
	private static String getDefaultHEXString(byte[] valueHex) {
		return HEXUtils.hexToString(valueHex);
	}
}
