package util;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import util.SBinHCStructs.SBinHCStruct;

public final class DataClasses {
	private DataClasses() {}
	
	public static class SBinJson {
		@SerializedName("FileName")
		private String fileName; 
		@SerializedName("SBinVersion")
		private int sbinVersion; 
		@SerializedName("SBinType")
		private SBinType sbinType; 
		@SerializedName("ENUM_HexStr")
		private String enumHexStr; 
		@SerializedName("ENUM_MidDATAStringsOrdering")
		private boolean enumMidDATAStringsOrdering = true; 
		@SerializedName("STRU_HexStr")
		private String struHexStr; 
		@SerializedName("FIEL_HexStr")
		private String fielHexStr; 
		@SerializedName("OHDR_HexStr")
		private String ohdrHexStr; 
		@SerializedName("DATA_HexStr")
		private String dataHexStr; 
		@SerializedName("DATA_LongElementIds")
		private boolean dataLongElementIds = false;
		@SerializedName("CHDR_HexStr")
		private String chdrHexStr; 
		@SerializedName("CDAT_HexStr")
		private String cdatHexStr;
		@SerializedName("BULK_HexStr")
		private String bulkHexStr;
		@SerializedName("BARG_HexStr")
		private String bargHexStr;
		@SerializedName("CDAT_AllStringsFromDATA")
		private boolean cdatAllStringsFromData = true;
		//
		@SerializedName("Enums")
		private List<SBinEnum> enums = new ArrayList<>();
		@SerializedName("EmptyFields")
		private List<SBinField> emptyFields = new ArrayList<>();
		@SerializedName("Structs")
		private List<SBinStruct> structs = new ArrayList<>();
		@SerializedName("DATA_Elements")
		private List<SBinDataElement> dataElements;
		@SerializedName("CDAT_Strings")
		private List<SBinCDATEntry> cdatStrings;
		
		public String getFileName() {
			return fileName;
		}
		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
		
		public int getSBinVersion() {
			return sbinVersion;
		}
		public void setSBinVersion(int sbinVersion) {
			this.sbinVersion = sbinVersion;
		}
		
		public SBinType getSBinType() {
			return sbinType;
		}
		public void setSBinType(SBinType sbinType) {
			this.sbinType = sbinType;
		}
		
		public String getENUMHexStr() {
			return enumHexStr;
		}
		public void setENUMHexStr(String enumHexStr) {
			this.enumHexStr = enumHexStr;
		}
		
		public boolean isENUMMidDATAStringsOrdering() {
			return enumMidDATAStringsOrdering;
		}
		public void setENUMMidDATAStringsOrdering(boolean enumMidDATAStringsOrdering) {
			this.enumMidDATAStringsOrdering = enumMidDATAStringsOrdering;
		}
		
		public String getSTRUHexStr() {
			return struHexStr;
		}
		public void setSTRUHexStr(String struHexStr) {
			this.struHexStr = struHexStr;
		}
		
		public String getFIELHexStr() {
			return fielHexStr;
		}
		public void setFIELHexStr(String fielHexStr) {
			this.fielHexStr = fielHexStr;
		}
		
		public String getOHDRHexStr() {
			return ohdrHexStr;
		}
		public void setOHDRHexStr(String ohdrHexStr) {
			this.ohdrHexStr = ohdrHexStr;
		}
		
		public String getDATAHexStr() {
			return dataHexStr;
		}
		public void setDATAHexStr(String dataHexStr) {
			this.dataHexStr = dataHexStr;
		}
		
		public boolean isDataLongElementIds() {
			return dataLongElementIds;
		}
		public void setDataLongElementIds(boolean dataLongElementIds) {
			this.dataLongElementIds = dataLongElementIds;
		}
		
		public String getCHDRHexStr() {
			return chdrHexStr;
		}
		public void setCHDRHexStr(String chdrHexStr) {
			this.chdrHexStr = chdrHexStr;
		}
		
		public String getCDATHexStr() {
			return cdatHexStr;
		}
		public void setCDATHexStr(String cdatHexStr) {
			this.cdatHexStr = cdatHexStr;
		}
		
		public String getBULKHexStr() {
			return bulkHexStr;
		}
		public void setBULKHexStr(String bulkHexStr) {
			this.bulkHexStr = bulkHexStr;
		}
		
		public String getBARGHexStr() {
			return bargHexStr;
		}
		public void setBARGHexStr(String bargHexStr) {
			this.bargHexStr = bargHexStr;
		}
		
		public boolean isCDATAllStringsFromDATA() {
			return cdatAllStringsFromData;
		}
		public void setCDATAllStringsFromDATA(boolean cdatAllStringsFromData) {
			this.cdatAllStringsFromData = cdatAllStringsFromData;
		}
		
		//
		
		public void addEnum(SBinEnum enumObj) {
			this.enums.add(enumObj);
		}
		
		public void addEmptyField(SBinField field) {
			this.emptyFields.add(field);
		}
		
		public void addStruct(SBinStruct struct) {
			this.structs.add(struct);
		}
		
		public List<SBinEnum> getEnums() {
			return enums;
		}
		public void setEnums(List<SBinEnum> enums) {
			this.enums = enums;
		}
		
		public List<SBinStruct> getStructs() {
			return structs;
		}
		public void setStructs(List<SBinStruct> structs) {
			this.structs = structs;
		}
		
		public List<SBinField> getEmptyFields() {
			return emptyFields;
		}
		public void setEmptyFields(List<SBinField> emptyFields) {
			this.emptyFields = emptyFields;
		}
		
		public List<SBinCDATEntry> getCDATStrings() {
			return cdatStrings;
		}
		public void setCDATStrings(List<SBinCDATEntry> cdatStrings) {
			this.cdatStrings = cdatStrings;
		}
		
		public List<SBinDataElement> getDataElements() {
			return dataElements;
		}
		public void setDataElements(List<SBinDataElement> dataElements) {
			this.dataElements = dataElements;
		}
	}
	
	public static class SBinCDATEntry {
		@SerializedName("CHDRHexId")
		private String chdrHexId;
		@SerializedName("String")
		private String string;
		
		public String getChdrHexId() {
			return chdrHexId;
		}
		public void setChdrHexId(String chdrHexId) {
			this.chdrHexId = chdrHexId;
		}
		
		public String getString() {
			return string;
		}
		public void setString(String string) {
			this.string = string;
		} 
	}
	
	public static class SBinEnum {
		@SerializedName("Id")
		private int id;
		@SerializedName("Name")
		private String name;
		@SerializedName("DataId_MapRef")
		private String dataIdMapRef;
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public String getDataIdMapRef() {
			return dataIdMapRef;
		}
		public void setDataIdMapRef(String dataIdMapRef) {
			this.dataIdMapRef = dataIdMapRef;
		}
	}
	
	public static class SBinStruct {
		@SerializedName("Id")
		private int id;
		@SerializedName("Name")
		private String name;
		@SerializedName("Size")
		private int size;
		@SerializedName("Fields")
		private List<SBinField> fieldsArray = new ArrayList<>();
		
		public void addToFields(SBinField field) {
			this.fieldsArray.add(field);
		}

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public int getSize() {
			return size;
		}
		public void setSize(int size) {
			this.size = size;
		}

		public List<SBinField> getFieldsArray() {
			return fieldsArray;
		}
		public void setFieldsArray(List<SBinField> fieldsArray) {
			this.fieldsArray = fieldsArray;
		}
	}
	
	public static class SBinField {
		@SerializedName("Name")
		private String name;
		@SerializedName("Type")
		private String type;
		@SerializedName("SubStruct")
		private String subStruct;
		@SerializedName("HexValue")
		private String hexValue;
		@SerializedName("Enum_JsonPreview")
		private String enumJsonPreview;
		@SerializedName("StartOffset")
		private int startOffset;
		@SerializedName("FieldSize")
		private int fieldSize = 0;
		@SerializedName("DynamicSize")
		private boolean dynamicSize = false;
		@SerializedName("SpecOrderId")
		private int specOrderId = 0;
		
		private SBinFieldType fieldTypeEnum;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		
		public String getSubStruct() {
			return subStruct;
		}
		public void setSubStruct(String subStruct) {
			this.subStruct = subStruct;
		}
		
		public String getHexValue() {
			return hexValue;
		}
		public void setHexValue(String hexValue) {
			this.hexValue = hexValue;
		}
		
		public String getEnumJsonPreview() {
			return enumJsonPreview;
		}
		public void setEnumJsonPreview(String enumJsonPreview) {
			this.enumJsonPreview = enumJsonPreview;
		}
		
		public int getStartOffset() {
			return startOffset;
		}
		public void setStartOffset(int startOffset) {
			this.startOffset = startOffset;
		}
		
		public int getFieldSize() {
			return fieldSize;
		}
		public void setFieldSize(int fieldSize) {
			this.fieldSize = fieldSize;
		}
		
		public boolean isDynamicSize() {
			return dynamicSize;
		}
		public void setDynamicSize(boolean dynamicSize) {
			this.dynamicSize = dynamicSize;
		}
		
		public int getSpecOrderId() {
			return specOrderId;
		}
		public void setSpecOrderId(int specOrderId) {
			this.specOrderId = specOrderId;
		}
		
		public SBinFieldType getFieldTypeEnum() {
			return fieldTypeEnum;
		}
		public void setFieldTypeEnum(SBinFieldType fieldTypeEnum) {
			this.fieldTypeEnum = fieldTypeEnum;
		}
	}
	
	public static class SBinDataElement {
		@SerializedName("OrderHexId")
		private String orderHexId; // Only for info
		@SerializedName("OHDRPadRemainder")
		private String ohdrPadRemainder = "0";
		@SerializedName("HexValue")
		private String hexValue;
		@SerializedName("ExtraHexValue")
		private String extraHexValue;
		@SerializedName("StructName")
		private String structName;
		@SerializedName("StructBaseName")
		private String structBaseName;
		@SerializedName("GlobalType")
		private SBinDataGlobalType globalType = SBinDataGlobalType.UNKNOWN;
		@SerializedName("MapElements")
		private List<String> mapElements;
		@SerializedName("Fields")
		private List<SBinDataField> fields;
		@SerializedName("ArrayObjects")
		private List<SBinDataElement> arrayObjects; // StructArray
		@SerializedName("HCStruct")
		private SBinHCStruct hcStruct; // Hard-coded Struct
		
		public String getOrderHexId() {
			return orderHexId;
		}
		public void setOrderHexId(String orderHexId) {
			this.orderHexId = orderHexId;
		}
		
		public int getOHDRPadRemainder() {
			return Integer.parseInt(ohdrPadRemainder);
		}
		public void setOHDRPadRemainder(int ohdrPadRemainder) {
			this.ohdrPadRemainder = String.valueOf(ohdrPadRemainder);
		}
		public void hideOHDRPadRemainder() {
			this.ohdrPadRemainder = null;
		}
		
		public String getHexValue() {
			return hexValue;
		}
		public void setHexValue(String hexValue) {
			this.hexValue = hexValue;
		}
		
		public String getExtraHexValue() {
			return extraHexValue;
		}
		public void setExtraHexValue(String extraHexValue) {
			this.extraHexValue = extraHexValue;
		}

		public String getStructName() {
			return structName;
		}
		public void setStructName(String structName) {
			this.structName = structName;
		}
		
		public String getStructBaseName() {
			return structBaseName;
		}
		public void setStructBaseName(String structBaseName) {
			this.structBaseName = structBaseName;
		}
		
		public SBinDataGlobalType getGlobalType() {
			return globalType;
		}
		public void setGlobalType(SBinDataGlobalType globalType) {
			this.globalType = globalType;
		}
		
		public List<String> getMapElements() {
			return mapElements;
		}
		public void setMapElements(List<String> mapElements) {
			this.mapElements = mapElements;
		}
		
		public List<SBinDataField> getFields() {
			return fields;
		}
		public void setFields(List<SBinDataField> fields) {
			this.fields = fields;
		}
		
		public List<SBinDataElement> getArrayObjects() {
			return arrayObjects;
		}
		public void setArrayObjects(List<SBinDataElement> arrayObjects) {
			this.arrayObjects = arrayObjects;
		}
		
		public SBinHCStruct getHCStruct() {
			return hcStruct;
		}
		public void setHCStruct(SBinHCStruct hcStruct) {
			this.hcStruct = hcStruct;
		}
	}
	
	public static class SBinDataField {
		@SerializedName("Name")
		private String name;
		@SerializedName("Type")
		private String type;
		@SerializedName("SubStruct")
		private String subStruct;
		@SerializedName("Enum_JsonPreview")
		private String enumJsonPreview;
		@SerializedName("EnumDataMapId_JsonPreview")
		private Long enumDataMapIdJsonPreview;
		@SerializedName("ForcedHexValue")
		private boolean forcedHexValue;
		@SerializedName("FieldSize")
		private int fieldSize = 0;
		@SerializedName("Value")
		private String value;
		@SerializedName("SubFields")
		private List<SBinDataField> subFields;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		
		public String getSubStruct() {
			return subStruct;
		}
		public void setSubStruct(String subStruct) {
			this.subStruct = subStruct;
		}
		
		public String getEnumJsonPreview() {
			return enumJsonPreview;
		}
		public void setEnumJsonPreview(String enumJsonPreview) {
			this.enumJsonPreview = enumJsonPreview;
		}
		
		public Long getEnumDataMapIdJsonPreview() {
			return enumDataMapIdJsonPreview;
		}
		public void setEnumDataMapIdJsonPreview(Long enumDataMapIdJsonPreview) {
			this.enumDataMapIdJsonPreview = enumDataMapIdJsonPreview;
		}
		
		public boolean isForcedHexValue() {
			return forcedHexValue;
		}
		public void setForcedHexValue(boolean forcedHexValue) {
			this.forcedHexValue = forcedHexValue;
		}
		
		public int getFieldSize() {
			return fieldSize;
		}
		public void setFieldSize(int fieldSize) {
			this.fieldSize = fieldSize;
		}
		
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		
		public List<SBinDataField> getSubFields() {
			return subFields;
		}
		public void setSubFields(List<SBinDataField> subFields) {
			this.subFields = subFields;
		}
	}
	
	public static class SBinHCStructFileArray {
		@SerializedName("Array")
		private List<String> fileNames = new ArrayList<>();

		public List<String> getFileNames() {
			return fileNames;
		}
		public void setFileNames(List<String> fileNames) {
			this.fileNames = fileNames;
		}
	}
	
}
