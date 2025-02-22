package util;

import java.util.HashMap;
import java.util.Map;

import util.DataClasses.SBinJson;

public class SBinMapUtils {
	private SBinMapUtils() {}
	
	public static final int HEADERENTRY_SIZE = 0x4;
	public static final int HEADERFULL_SIZE = 0x8;
	
	private static Map<Integer, SBinMapType> mapTypes = new HashMap<>();
	
	public static void initMapTypes() {
		addMapType(new SBinMapType(0xD, "EnumMap", 0x2, true, false, true));
		addMapType(new SBinMapType(0xF, "DataIdsMap", 0x4, false, false, false));
		addMapType(new SBinMapType(0x10, "StringTable", 0x8, false, true, false));
	}
	
	private static void addMapType(SBinMapType mapType) {
		mapTypes.putIfAbsent(mapType.getTypeId(), mapType);
	}
	
	public static SBinMapType getMapType(Integer id, SBinJson sbinJson) {
		SBinMapType type = mapTypes.getOrDefault(id, null);
		if (type != null && type.isEnumMap() && sbinJson.getEnums().isEmpty()) {
			return null; // Different files can have a varied header rules
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
	
	//
	
	public static class SBinMapType {
		private int typeId;
		private String typeName;
		private int entrySize;
		private boolean isEnumMap;
		private boolean isStringDataMap;
		private boolean cdatEntries;
		
		public SBinMapType(int typeId, String typeName, int entrySize,
				boolean isEnumMap, boolean isStringDataMap, boolean cdatEntries) {
			this.typeId = typeId;
			this.typeName = typeName;
			this.entrySize = entrySize;
			this.isEnumMap = isEnumMap;
			this.isStringDataMap = isStringDataMap;
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

		public boolean isStringDataMap() {
			return isStringDataMap;
		}
		public void setStringDataMap(boolean isStringDataMap) {
			this.isStringDataMap = isStringDataMap;
		}

		public boolean isCDATEntries() {
			return cdatEntries;
		}
		public void setCDATEntries(boolean cdatEntries) {
			this.cdatEntries = cdatEntries;
		}
		
	}
}
