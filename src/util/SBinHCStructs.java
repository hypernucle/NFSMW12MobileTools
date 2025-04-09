package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import util.DataClasses.SBinDataElement;
import util.json.JsonClassSubType;
import util.json.JsonClassType;

public class SBinHCStructs {
	private SBinHCStructs() {}
	
	private static final String HCS_TYPE_PROPSBASE = "PropertiesBase";
	private static final int PROPSBASE_HEADER_SIZE = 0x4;
	//
	private static final String HCS_TYPE_INTMAP = "IntegerMap";
	private static final int INTMAP_HEADER_SIZE = 0x8;
	private static final int INTMAP_HEADER_ID = 0x5;
	
	// ! That Switch & Case structure is done here intentionally, due to unknown and varied Struct formation methods from file to file
	
	//
	//
	//
	
	public static boolean unpackHCStructs(byte[] elementHex, SBinDataElement element, int structId) {
		boolean isHCStruct = false;
		
		switch(SBJson.get().getSBinType()) {
		case CAR_CONFIG: case HCSTRUCTS_COMMON:
			if (structId != 0xF || !SBinMapUtils.isMapPropertiesValid(elementHex)) { // Regular Map Array?
				unpackPropertiesBaseObj(elementHex, element);
				isHCStruct = true;
			}
 			break;
		case LAYOUTS:
			
			switch(structId) {
			case 0x1: case 0xC:
				unpackPropertiesBaseObj(elementHex, element);
				isHCStruct = true;
				break;
			case 0x5:
				System.out.println(element.getOrderHexId());
				if (checkForIntegerMap(structId, elementHex)) {
					unpackIntegerMap(elementHex, element);
					isHCStruct = true;
				}
				break;
			default: break;
			}
			
 			break;
		case TWEAKS:
			if (checkForIntegerMap(structId, elementHex)) {
				unpackIntegerMap(elementHex, element);
				isHCStruct = true;
			} else if (structId != 0xF || !SBinMapUtils.isMapPropertiesValid(elementHex)) { // Regular Map Array?
				unpackPropertiesBaseObj(elementHex, element);
				isHCStruct = true;
			}
			break;
		default: 
			if (structId == 0x1) {
				unpackPropertiesBaseObj(elementHex, element);
				isHCStruct = true;
			}
			break;
		}
		return isHCStruct;
	}
	
	public static byte[] repackHCStructs(SBinDataElement element) throws IOException {
		byte[] hcStructBytes = null;
		switch(SBJson.get().getSBinType()) {
		case CAR_CONFIG: case HCSTRUCTS_COMMON: case TWEAKS: case LAYOUTS:
			
			switch(element.getHCStruct().getType()) {
			case HCS_TYPE_INTMAP:
				hcStructBytes = repackIntegerMapObj(element);
				break;
			default: 
				hcStructBytes = repackPropertiesBaseObj(element);
				break;
			}
			
 			break;
		default: 
			if (element.getHCStruct().getType().contentEquals(HCS_TYPE_PROPSBASE)) {
				hcStructBytes = repackPropertiesBaseObj(element);
			}
			break;
		}
		if (hcStructBytes == null) {
			throw new NullPointerException("!!! HC Struct for Element Id " + element.getOrderHexId() + " is referenced, but does not exist.");
		}
		return hcStructBytes;
	}
	
	// Sometimes one Struct Id could be re-used for both SBin & Hardcoded structs in the same file
	// Values include possible 1/2 byte padding
	public static boolean isExceptionForHCStructs(byte[] elementHex, int structIdCount) {
		boolean notHCStruct = false;
		switch(SBJson.get().getSBinType()) {
		case CAR_CONFIG:
			
			switch(structIdCount) {
			case 0x1:
				notHCStruct = elementHex.length < 0xD;
				break;
			case 0x2:
				notHCStruct = elementHex.length < 0x9;
				break;
			default: break;
			}
			
 			break;
		case LAYOUTS:
			
			switch(structIdCount) {
			case 0x1:
				notHCStruct = elementHex.length < 0xD;
				break;
			case 0xC:
				notHCStruct = elementHex.length < 0x13;
				break;
			default: break;
			}
			
 			break;
		default: 
			if (structIdCount == 0x1) {
				notHCStruct = (elementHex.length != 0x10 && elementHex.length != 0x12)
						|| HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 2, 4)) > 0xFF // ... too big?
						|| elementHex[6] > 0x16; // ... too high field type?
			} // Exists in many other SBin files, usually on first slot
			break;
		}
		return notHCStruct;
	}
	
	//
	//
	//
	
	private static void unpackPropertiesBaseObj(byte[] elementHex, SBinDataElement element) {
		SBHCSPropertiesBase propsObj = 
				(SBHCSPropertiesBase)setHCStructObjParameters(element, new SBHCSPropertiesBase());
		propsObj.setType(HCS_TYPE_PROPSBASE);
		int byteSize = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 2, 4));
		checkDATAPadding(elementHex, byteSize, element);
		
		propsObj.setProperties(readPropertyList(PROPSBASE_HEADER_SIZE, byteSize, elementHex));
	}
	
	private static List<SBHCSPropertyEntity> readPropertyList(
			int headerSize, int elementRealSize, byte[] elementHex) {
		//System.out.println(HEXUtils.hexToString(elementHex));
		List<SBHCSPropertyEntity> props = new ArrayList<>();
		int bytesTaken = headerSize; // First byte is objects counter
		
		while (bytesTaken < elementRealSize) {
			SBHCSPropertyEntity entryObj = new SBHCSPropertyEntity();

			entryObj.setPropertyNameCHDR(DataUtils.getCDATStringByShortCHDRId(elementHex, bytesTaken, bytesTaken + 2));
			bytesTaken += 2;
			//
			entryObj.setValueType(SBinFieldType.valueOf(
					HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 2))));
			bytesTaken += 2;
			//
			int offsetCounter = HEXUtils.byteArrayToInt(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 4));
			entryObj.setOffsetCounter(offsetCounter);
			bytesTaken += 4;
			if (bytesTaken != offsetCounter) { // ... padding can happen here for some reason
				bytesTaken += offsetCounter - bytesTaken;
			}
			
			switch(entryObj.getValueType()) {
			case INT32: case U_INT32:
				entryObj.setValue(String.valueOf(HEXUtils.byteArrayToInt(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 4))));
				bytesTaken += 4;
				break;
			case FLOAT:
				entryObj.setValue(String.valueOf(HEXUtils.bytesToFloat(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 4))));
				bytesTaken += 4;
				break;
			case BOOLEAN:
				int valueInt = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 4));
				entryObj.setValue(Boolean.toString(valueInt == 1));
				bytesTaken += 2;
				break;
			case CHDR_ID_REF:
				entryObj.setValue(DataUtils.getCDATStringByShortCHDRId(elementHex, bytesTaken, bytesTaken + 2)); 
				bytesTaken += 2;
				break;
			default: 
				entryObj.setValue(HEXUtils.hexToString(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 4))); 
				bytesTaken += 4; // DATA Id reference or other stuff
				break;
			}
			props.add(entryObj);
		}
		
		return props;
	}
	
	private static void unpackIntegerMap(byte[] elementHex, SBinDataElement element) {
		SBHCSIntegerMap intMapObj = 
				(SBHCSIntegerMap)setHCStructObjParameters(element, new SBHCSIntegerMap());
		intMapObj.setType(HCS_TYPE_INTMAP);
		int mapSize = HEXUtils.byteArrayToInt(Arrays.copyOfRange(elementHex, 4, 8));
		checkDATAPadding(elementHex, INTMAP_HEADER_SIZE + (mapSize * 0x4), element);
		
		List<Integer> intMap = new ArrayList<>();
		for (int i = 0; i < mapSize; i++) {
			int startPos = INTMAP_HEADER_SIZE + (0x4 * i);
			intMap.add(HEXUtils.byteArrayToInt(Arrays.copyOfRange(elementHex, startPos, startPos + 0x4)));
		}
		intMapObj.setIntegerMap(intMap);
	}
	
	//
	//
	//
	
	private static byte[] repackPropertiesBaseObj(SBinDataElement element) throws IOException {
		SBHCSPropertiesBase propsJson = (SBHCSPropertiesBase)element.getHCStruct();
		
		int bytesTaken = PROPSBASE_HEADER_SIZE; // First byte is objects counter
		ByteArrayOutputStream propsBytes = new ByteArrayOutputStream();
		for (SBHCSPropertyEntity property : propsJson.getProperties()) {
			propsBytes.write(DataUtils.processStringInCDAT(property.getPropertyNameCHDR()));
			propsBytes.write(HEXUtils.shortToBytes(property.getValueType().getId()));
			propsBytes.write(HEXUtils.intToByteArrayLE(property.getOffsetCounter()));
			bytesTaken += 8; // 2 + 2 + 4
			if (property.getOffsetCounter() != bytesTaken) {
				int pad = property.getOffsetCounter() - bytesTaken;
				propsBytes.write(new byte[pad]);
				bytesTaken += pad;
			}
			
			switch(property.getValueType()) {
			case INT32: case U_INT32:
				propsBytes.write(HEXUtils.intToByteArrayLE(Integer.parseInt(property.getValue())));
				bytesTaken += 4;
				break;
			case FLOAT:
				propsBytes.write(HEXUtils.floatToBytes(Float.parseFloat(property.getValue())));
				bytesTaken += 4;
				break;
			case BOOLEAN:
				int bool = Boolean.parseBoolean(property.getValue()) ? 0x1 : 0x0;
				propsBytes.write(new byte[] {(byte)bool, 0x0});
				bytesTaken += 2;
				break;
			case CHDR_ID_REF:
				propsBytes.write(DataUtils.processStringInCDAT(property.getValue()));
				bytesTaken += 2;
				break;
			default:
				propsBytes.write(HEXUtils.decodeHexStr(property.getValue()));
				bytesTaken += 4;
				break;
			}
		}
		
		ByteArrayOutputStream finalPropsBytes = new ByteArrayOutputStream();
		finalPropsBytes.write(HEXUtils.shortToBytes(propsJson.getProperties().size()));
		finalPropsBytes.write(HEXUtils.shortToBytes(PROPSBASE_HEADER_SIZE + propsBytes.size())); // Full element size
		finalPropsBytes.write(propsBytes.toByteArray());
		return finalPropsBytes.toByteArray();
	}
	
	private static byte[] repackIntegerMapObj(SBinDataElement element) throws IOException {
		SBHCSIntegerMap intMapJson = (SBHCSIntegerMap)element.getHCStruct();
		
		ByteArrayOutputStream mapBytes = new ByteArrayOutputStream();
		mapBytes.write(HEXUtils.intToByteArrayLE(INTMAP_HEADER_ID));
		mapBytes.write(HEXUtils.intToByteArrayLE(intMapJson.getIntegerMap().size()));
		
		for (Integer intObj : intMapJson.getIntegerMap()) {
			mapBytes.write(HEXUtils.intToByteArrayLE(intObj));
		}
		return mapBytes.toByteArray();
	}

	//
	//
	//
	
	private static SBinHCStruct setHCStructObjParameters(SBinDataElement element, SBinHCStruct hcStruct) {
		element.setGlobalType(SBinDataGlobalType.HC_STRUCT);
		element.setHCStruct(hcStruct);
		return element.getHCStruct();
	}
	
	private static boolean checkForIntegerMap(int structId, byte[] elementHex) {
		return structId == 0x5 
				&& elementHex.length > 7
				&& HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 2, 4)) == 0x00
				&& HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 6, 8)) == 0x00;
	}
	
	private static void checkDATAPadding(byte[] elementHex, int bytesTaken, SBinDataElement element) {
		if (bytesTaken != elementHex.length) {
			element.setExtraHexValue(HEXUtils.hexToString(Arrays.copyOfRange(
					elementHex, bytesTaken, elementHex.length)));
		}
	}
	
	//
	//
	//
	
	public static class SBHCSIntegerMap extends SBinHCStruct {
		@SerializedName("Map")
		private List<Integer> map;
		
		@Override
		public SBinHCStruct newClass() {
			return new SBHCSIntegerMap();
		}

		public List<Integer> getIntegerMap() {
			return map;
		}
		public void setIntegerMap(List<Integer> map) {
			this.map = map;
		}
	}
	
	public static class SBHCSPropertiesBase extends SBinHCStruct {
		@SerializedName("Properties")
		private List<SBHCSPropertyEntity> properties;
		
		@Override
		public SBinHCStruct newClass() {
			return new SBHCSPropertiesBase();
		}

		public List<SBHCSPropertyEntity> getProperties() {
			return properties;
		}
		public void setProperties(List<SBHCSPropertyEntity> properties) {
			this.properties = properties;
		}
	}
	
	public static class SBHCSPropertyEntity {
		@SerializedName("PropertyNameCHDR")
		private String propertyNameCHDR;
		@SerializedName("ValueType")
		private SBinFieldType valueType;
		@SerializedName("OffsetCounter")
		private int offsetCounter;
		@SerializedName("Value")
		private String value;
		
		public String getPropertyNameCHDR() {
			return propertyNameCHDR;
		}
		public void setPropertyNameCHDR(String propertyNameCHDR) {
			this.propertyNameCHDR = propertyNameCHDR;
		}
		
		public SBinFieldType getValueType() {
			return valueType;
		}
		public void setValueType(SBinFieldType valueType) {
			this.valueType = valueType;
		}
		
		public int getOffsetCounter() {
			return offsetCounter;
		}
		public void setOffsetCounter(int offsetCounter) {
			this.offsetCounter = offsetCounter;
		}
		
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}
	
	@JsonClassType(property = "Type",
		subTypes = {
			@JsonClassSubType(jsonClass = SBHCSPropertiesBase.class, name = HCS_TYPE_PROPSBASE),
			@JsonClassSubType(jsonClass = SBHCSIntegerMap.class, name = HCS_TYPE_INTMAP)
		}
	)
	public static class SBinHCStruct {
		@SerializedName("Type")
		private String type;
		
		public SBinHCStruct newClass() {
			return new SBinHCStruct();
		}
		
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
	}
}
