package util;

import util.DataClasses.SBinDataField;
import util.DataClasses.SBinEnum;
import util.DataClasses.SBinField;
import util.DataClasses.SBinJson;

public class SBinEnumUtils {
	
	//
	// SBinFieldType
	//
	
	private static final String UNK_PREFIX = "UNK_";
	
	public static int getFieldStandardSize(SBinFieldType type) {
		int size = 0x0;
		if (type == null) {return size;}
		switch(type) {
		case INT8: case U_INT8:
			size = 0x1;
			break;
		case BOOLEAN: case CHDR_ID_REF: case CHDR_SYMBOL_ID_REF:
			size = 0x2;
			break;
		case INT32: case U_INT32: case FLOAT: case DATA_ID_REF: case DATA_ID_MAP: case ENUM_ID_INT32: case BULK_OFFSET_ID:
			size = 0x4;
			break;
		case DOUBLE:
			size = 0x8;
			break;
		default: break;
		}
		return size;
	}
	
	public static String formatFieldValueUnpack(
			SBinField field, SBinDataField dataField, int fieldRealSize, byte[] valueHex, SBinJson sbinJson) {
		String strValue;
		// Try to handle int, boolean, float or CHDR ref with weird field size, but force last elements as a plain HEX to be safe
		if (field.getFieldTypeEnum() == null ||
				( getFieldStandardSize(field.getFieldTypeEnum()) != fieldRealSize && field.isDynamicSize()) ) {
			return getDefaultHEXString(valueHex, dataField);
		}
		switch(field.getFieldTypeEnum()) {
		case INT32: case U_INT32: case ENUM_ID_INT32: case BULK_OFFSET_ID:
			// ENUM_ID_INT32: Enum stores all values on the last elements of DATA block, 
			// so we read their values on other function.
			strValue = String.valueOf(HEXUtils.byteArrayToInt(valueHex));
			break;
		case FLOAT:
			strValue = String.valueOf(HEXUtils.bytesToFloat(valueHex));
			break;
		case DOUBLE:
			strValue = String.valueOf(HEXUtils.bytesToDouble(valueHex));
			break;
		case BOOLEAN: // Boolean sizes can be really different
			int valueInt = valueHex.length == 1 ? valueHex[0] : HEXUtils.twoLEByteArrayToInt(valueHex);
			if (valueInt < 2) { // Precaution in case of unknown value type
				strValue = Boolean.toString(valueInt == 1);
			} else {
				strValue = getDefaultHEXString(valueHex, dataField);
			}
			break;
		case CHDR_ID_REF: case CHDR_SYMBOL_ID_REF:
			strValue = sbinJson.getCDATStrings().get(HEXUtils.twoLEByteArrayToInt(valueHex)).getString();
			break;
		case INT8: case U_INT8: case DATA_ID_REF: case DATA_ID_MAP: default: 
			// U_INT8: Primarily used for HEX colors, left as it is
			// DATA_ID: simpler to provide HEX code and compare/find with other DATA info
			strValue = getDefaultHEXString(valueHex, dataField);
			break;
		}
		return strValue;
	}
	
	public static byte[] convertValueByType(
			SBinFieldType type, SBinDataField dataField, SBinJson sbinJson, int fieldRealSize) {
		byte[] value = new byte[0];
		switch(type) {
		case INT32: case U_INT32: case BULK_OFFSET_ID:
			value = HEXUtils.intToByteArrayLE(Integer.parseInt(dataField.getValue()), 0x4);
			break;
		case FLOAT:
			value = HEXUtils.floatToBytes(Float.parseFloat(dataField.getValue()));
			break;
		case DOUBLE:
			value = HEXUtils.doubleToBytes(Double.parseDouble(dataField.getValue()));
			break;
		case BOOLEAN: 
			boolean bool = Boolean.parseBoolean(dataField.getValue());
			int boolInt = bool ? 1 : 0;
			value = HEXUtils.shortToBytes((short)boolInt);
			if (fieldRealSize == 1) {
				value = new byte[]{value[0]};
			} 
			break;
		case CHDR_ID_REF: case CHDR_SYMBOL_ID_REF:
			value = DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), dataField.getValue());
			break;
		case ENUM_ID_INT32:
			value = getEnumValueBytes(sbinJson, dataField);
			break;
		case INT8: case U_INT8: case DATA_ID_REF: case DATA_ID_MAP: default: 
			value = HEXUtils.decodeHexStr(dataField.getValue());
			break;
		}
		return value;
	}
	
	private static String getDefaultHEXString(byte[] valueHex, SBinDataField dataField) {
		dataField.setForcedHexValue(true);
		return HEXUtils.hexToString(valueHex);
	}
	
	public static int getIdByStringName(String enumStr) {
		boolean isUnknownType = enumStr.startsWith(UNK_PREFIX);
		String enumStrCheck = isUnknownType ? 
				enumStr.substring(UNK_PREFIX.length(), enumStr.length()) : enumStr;
		if (isUnknownType) { 
			return Integer.decode(enumStrCheck); 
		} else {
			return SBinFieldType.valueOf(enumStrCheck).getId();
		}
	}
	
	private static byte[] getEnumValueBytes(SBinJson sbinJson, SBinDataField dataField) {
		int enumMapId = 0;
		for (SBinEnum enumObj : sbinJson.getEnums()) {
			if (enumObj.getName().contentEquals(dataField.getEnumJsonPreview())) {
				enumMapId = HEXUtils.twoLEByteArrayToInt(
						HEXUtils.decodeHexStr(enumObj.getDataIdMapRef()));
			}
		}
		int i = 0;
		for (String mapElement : sbinJson.getDataElements().get(enumMapId).getMapElements()) {
			if (mapElement.contentEquals(dataField.getValue())) {
				return HEXUtils.intToByteArrayLE(i, 0x4);
			}
			i++;
		}
		System.out.println("!!! [Enums] Unable to getEnumValueBytes for Enum " 
				+ dataField.getEnumJsonPreview() + ": default 2 empty bytes is applied instead.");
		return new byte[2];
	}
}
