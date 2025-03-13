package util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import util.DataClasses.SBinField;

public class SBinMapUtils {
	private SBinMapUtils() {}
	
	public static final int HEADERENTRY_SIZE = 0x4;
	public static final int HEADERFULL_SIZE = 0x8;
	private static final byte[] SHORTBYTE_EMPTY = new byte[2];
	public static final String STRUCT_ARRAY_RULE = "StructArrayRule";
	
	private static Map<Integer, SBinMapType> mapTypes = new HashMap<>();
	
	public static void initMapTypes() {
		addMapType(new SBinMapType(0xD, "EnumMap", 0x2, true, false, true));
		addMapType(new SBinMapType(0xF, "DataIdsMap", 0x4, false, false, false));
		addMapType(new SBinMapType(0x10, "StructArray", 0x8, false, true, false));
	}
	
	private static void addMapType(SBinMapType mapType) {
		mapTypes.putIfAbsent(mapType.getTypeId(), mapType);
	}
	
	public static SBinMapType getMapType(byte[] elementHex) {
		int id = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 0, 2));
		SBinMapType type = mapTypes.getOrDefault(id, null);
		if (type == null) {return type;}
		
		// Different files can have a varied header rules
		if (!type.isStructArray() && !Arrays.equals(Arrays.copyOfRange(elementHex, 2, 4), SHORTBYTE_EMPTY)) {
			return null; // Struct Array header is 2 bytes, with other 2 bytes indicating Struct Id
		}
		if (type.getTypeId() == 0xF && !isMapPropertiesValid(elementHex)) {
			return null; 
		}	
		if (type.isEnumMap() && SBJson.get().getEnums().isEmpty()) {
			return null; 
		}
		if (type.isStructArray()) {
			boolean structsExists = SBJson.get().getStructs() != null;
			if (!structsExists || !checkForArrayRuleField(elementHex)) {
				return null; // SubStruct enum is chosen just because of the same Id
			}
		}
		return type;
	}
	
	public static SBinMapType getMapType(String typeName) {
		for (SBinMapType type : mapTypes.values()) {
			if (type.getTypeName().contentEquals(typeName)) {
				return type;
			}
		}
		return null;
	}
	
	public static boolean checkForArrayRuleField(byte[] elementHex) {
		if (!isMapPropertiesValid(elementHex)) {return false;}
		int structBaseId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 2, 4));
		for (SBinField field : SBJson.get().getEmptyFields()) {
			if (field.getName().contentEquals(STRUCT_ARRAY_RULE) && field.getSpecOrderId() == structBaseId) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isMapPropertiesValid(byte[] elementHex) {
		int arrayCount = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 4, 6));
		if (HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 6, 8)) != 0x0
				|| arrayCount > 0x1000  
				|| (arrayCount == 0x0 && elementHex.length > 0xA) ) {
			return false; // Object of Struct? Something else?
		}
		return true;
	}
	
	//
	
	public static class SBinMapType {
		private int typeId;
		private String typeName;
		private int entrySize;
		private boolean isEnumMap;
		private boolean isStructArray;
		private boolean cdatEntries;
		
		public SBinMapType(int typeId, String typeName, int entrySize,
				boolean isEnumMap, boolean isStructArray, boolean cdatEntries) {
			this.typeId = typeId;
			this.typeName = typeName;
			this.entrySize = entrySize;
			this.isEnumMap = isEnumMap;
			this.isStructArray = isStructArray;
			this.cdatEntries = cdatEntries;
		}
		
		public int getTypeId() {
			return typeId;
		}
		public void setTypeId(int typeId) {
			this.typeId = typeId;
		}
		
		public String getTypeName() {
			return typeName;
		}
		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}
		
		public int getEntrySize() {
			return entrySize;
		}
		public void setEntrySize(int entrySize) {
			this.entrySize = entrySize;
		}

		public boolean isEnumMap() {
			return isEnumMap;
		}
		public void setEnumMap(boolean isEnumMap) {
			this.isEnumMap = isEnumMap;
		}

		public boolean isStructArray() {
			return isStructArray;
		}
		public void setStructArray(boolean isStructArray) {
			this.isStructArray = isStructArray;
		}

		public boolean isCDATEntries() {
			return cdatEntries;
		}
		public void setCDATEntries(boolean cdatEntries) {
			this.cdatEntries = cdatEntries;
		}

	}
}
