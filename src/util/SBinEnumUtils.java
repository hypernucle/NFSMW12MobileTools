package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import util.DataClasses.SBinDataField;
import util.DataClasses.SBinEnum;
import util.DataClasses.SBinField;

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
		case INT16: case U_INT16:
			size = 0x2;
			break;
		case INT32: case U_INT32: case FLOAT: case ENUM_ID_INT32: case BULK_OFFSET_ID:
			size = 0x4;
			break;
		case DOUBLE:
			size = 0x8;
			break;
		default: case BOOLEAN: case DATA_ID_REF: case DATA_ID_MAP: 
			case CHDR_ID_REF: case CHDR_SYMBOL_ID_REF: break;
		}
		return size;
	}
	
	public static String formatFieldValueUnpack(SBinField field, SBinDataField dataField, byte[] valueHex) {
		if (field.getFieldTypeEnum() != null) {
			int fieldStandardSize = getFieldStandardSize(field.getFieldTypeEnum());
			// Try to handle values with weird field size - probably the object or type has been detected wrong
			if (fieldStandardSize != 0x0 && fieldStandardSize != valueHex.length) {
				return getDefaultHEXString(valueHex, dataField);
			}
		} else {
			return getDefaultHEXString(valueHex, dataField);
		}
		
		String strValue;
		switch(field.getFieldTypeEnum()) {
		case INT32: case U_INT32: case ENUM_ID_INT32: case BULK_OFFSET_ID:
			// ENUM_ID_INT32: Enum stores all values on the last elements of DATA block, 
			// so we read their values on other function.
			strValue = String.valueOf(HEXUtils.byteArrayToInt(valueHex));
			break;
		case INT16: case U_INT16:
			strValue = String.valueOf(HEXUtils.bytesToShort(valueHex));
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
		case CHAR: // Char being stored on DATA block
			strValue = String.valueOf((char)valueHex[0]);
			break;
		case CHDR_ID_REF: case CHDR_SYMBOL_ID_REF:
			// CHDR usually is 2 bytes long. Symbol CHDR can be up to 4 bytes
			byte[] firstTwoBytes = Arrays.copyOfRange(valueHex, 0, 2);
			strValue = SBJson.get().getCDATStrings().get(HEXUtils.twoLEByteArrayToInt(firstTwoBytes)).getString();
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
			SBinFieldType type, SBinDataField dataField, int fieldRealSize) throws IOException {
		byte[] value = new byte[0];
		switch(type) {
		case INT32: case U_INT32: case BULK_OFFSET_ID:
			value = HEXUtils.intToByteArrayLE(Integer.parseInt(dataField.getValue()));
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
			ByteArrayOutputStream boolValueStream = new ByteArrayOutputStream();
			boolValueStream.write((byte)boolInt);
			boolValueStream.write(new byte[fieldRealSize - 0x1]);
			value = boolValueStream.toByteArray();
			break;
		case CHAR: // Char being stored on DATA block
			char valueChar = dataField.getValue().charAt(0);
			ByteArrayOutputStream charValueStream = new ByteArrayOutputStream();
			charValueStream.write(valueChar);
			charValueStream.write(new byte[fieldRealSize - 0x1]);
			value = charValueStream.toByteArray();
			break;
		case CHDR_ID_REF: case CHDR_SYMBOL_ID_REF:
			value = DataUtils.processStringInCDAT(dataField.getValue());
			if (fieldRealSize > 0x2) {
				ByteArrayOutputStream valueStream = new ByteArrayOutputStream();
				valueStream.write(value);
				valueStream.write(new byte[fieldRealSize - value.length]);
				value = valueStream.toByteArray();
			}
			break;
		case ENUM_ID_INT32:
			value = getEnumValueBytes(dataField);
			break;
		case INT8: case U_INT8: case DATA_ID_REF: case DATA_ID_MAP: default: 
			value = HEXUtils.decodeHexStr(dataField.getValue());
			break;
		}
		return value;
	}
	
	//
	//
	//
	
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
	
	private static byte[] getEnumValueBytes(SBinDataField dataField) {
		int enumMapId = 0;
		for (SBinEnum enumObj : SBJson.get().getEnums()) {
			if (enumObj.getName().contentEquals(dataField.getEnumJsonPreview())) {
				enumMapId = HEXUtils.twoLEByteArrayToInt(
						HEXUtils.decodeHexStr(enumObj.getDataIdMapRef()));
			}
		}
		int i = 0;
		for (String mapElement : SBJson.get().getDataElements().get(enumMapId).getMapElements()) {
			if (mapElement.contentEquals(dataField.getValue())) {
				return HEXUtils.intToByteArrayLE(i);
			}
			i++;
		}
		System.out.println("!!! [Enums] Unable to getEnumValueBytes for Enum " 
				+ dataField.getEnumJsonPreview() + ": default 2 empty bytes is applied instead.");
		return new byte[2];
	}
}
