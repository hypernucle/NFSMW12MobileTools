package util;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

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
		@SerializedName("ENUM_HexEmptyBytesCount")
		private Long enumHexEmptyBytesCount; 
		@SerializedName("ENUM_MidDATAStringsOrdering")
		private boolean enumMidDATAStringsOrdering = true; 
		@SerializedName("STRU_HexStr")
		private String struHexStr; 
		@SerializedName("STRU_HexEmptyBytesCount")
		private Long struHexEmptyBytesCount; 
		@SerializedName("FIEL_HexStr")
		private String fielHexStr; 
		@SerializedName("FIEL_HexEmptyBytesCount")
		private Long fielHexEmptyBytesCount; 
		@SerializedName("OHDR_HexStr")
		private String ohdrHexStr; 
		@SerializedName("OHDR_HexEmptyBytesCount")
		private Long ohdrHexEmptyBytesCount; 
		@SerializedName("DATA_HexStr")
		private String dataHexStr; 
		@SerializedName("DATA_HexEmptyBytesCount")
		private Long dataHexEmptyBytesCount; 
		@SerializedName("CHDR_HexStr")
		private String chdrHexStr; 
		@SerializedName("CHDR_HexEmptyBytesCount")
		private Long chdrHexEmptyBytesCount; 
		@SerializedName("CDAT_HexStr")
		private String cdatHexStr;
		@SerializedName("CDAT_HexEmptyBytesCount")
		private Long cdatHexEmptyBytesCount;
		@SerializedName("BULK_HexStr")
		private String bulkHexStr;
		@SerializedName("BULK_HexEmptyBytesCount")
		private Long bulkHexEmptyBytesCount;
		@SerializedName("BARG_HexStr")
		private String bargHexStr;
		@SerializedName("BARG_HexEmptyBytesCount")
		private Long bargHexEmptyBytesCount;
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
		@SerializedName("PlaylistsData")
		private List<SBinPlaylistObj> playlistsArray;
		
		public String getFileName() {
			return fileName;
		}
		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
		
		public int getSbinVersion() {
			return sbinVersion;
		}
		public void setSbinVersion(int sbinVersion) {
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
		
		public Long getENUMHexEmptyBytesCount() {
			return enumHexEmptyBytesCount;
		}
		public void setENUMHexEmptyBytesCount(Long enumHexEmptyBytesCount) {
			this.enumHexEmptyBytesCount = enumHexEmptyBytesCount;
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
		
		public Long getSTRUHexEmptyBytesCount() {
			return struHexEmptyBytesCount;
		}
		public void setSTRUHexEmptyBytesCount(Long struHexEmptyBytesCount) {
			this.struHexEmptyBytesCount = struHexEmptyBytesCount;
		}
		
		public String getFIELHexStr() {
			return fielHexStr;
		}
		public void setFIELHexStr(String fielHexStr) {
			this.fielHexStr = fielHexStr;
		}
		
		public Long getFIELHexEmptyBytesCount() {
			return fielHexEmptyBytesCount;
		}
		public void setFIELHexEmptyBytesCount(Long fielHexEmptyBytesCount) {
			this.fielHexEmptyBytesCount = fielHexEmptyBytesCount;
		}
		
		public String getOHDRHexStr() {
			return ohdrHexStr;
		}
		public void setOHDRHexStr(String ohdrHexStr) {
			this.ohdrHexStr = ohdrHexStr;
		}
		
		public Long getOHDRHexEmptyBytesCount() {
			return ohdrHexEmptyBytesCount;
		}
		public void setOHDRHexEmptyBytesCount(Long ohdrHexEmptyBytesCount) {
			this.ohdrHexEmptyBytesCount = ohdrHexEmptyBytesCount;
		}
		
		public String getDATAHexStr() {
			return dataHexStr;
		}
		public void setDATAHexStr(String dataHexStr) {
			this.dataHexStr = dataHexStr;
		}
		
		public Long getDATAHexEmptyBytesCount() {
			return dataHexEmptyBytesCount;
		}
		public void setDATAHexEmptyBytesCount(Long dataHexEmptyBytesCount) {
			this.dataHexEmptyBytesCount = dataHexEmptyBytesCount;
		}
		
		public String getCHDRHexStr() {
			return chdrHexStr;
		}
		public void setCHDRHexStr(String chdrHexStr) {
			this.chdrHexStr = chdrHexStr;
		}
		
		public Long getCHDRHexEmptyBytesCount() {
			return chdrHexEmptyBytesCount;
		}
		public void setCHDRHexEmptyBytesCount(Long chdrHexEmptyBytesCount) {
			this.chdrHexEmptyBytesCount = chdrHexEmptyBytesCount;
		}
		
		public String getCDATHexStr() {
			return cdatHexStr;
		}
		public void setCDATHexStr(String cdatHexStr) {
			this.cdatHexStr = cdatHexStr;
		}
		
		public Long getCDATHexEmptyBytesCount() {
			return cdatHexEmptyBytesCount;
		}
		public void setCDATHexEmptyBytesCount(Long cdatHexEmptyBytesCount) {
			this.cdatHexEmptyBytesCount = cdatHexEmptyBytesCount;
		}
		
		public String getBULKHexStr() {
			return bulkHexStr;
		}
		public void setBULKHexStr(String bulkHexStr) {
			this.bulkHexStr = bulkHexStr;
		}
		
		public Long getBULKHexEmptyBytesCount() {
			return bulkHexEmptyBytesCount;
		}
		public void setBULKHexEmptyBytesCount(Long bulkHexEmptyBytesCount) {
			this.bulkHexEmptyBytesCount = bulkHexEmptyBytesCount;
		}
		
		public String getBARGHexStr() {
			return bargHexStr;
		}
		public void setBARGHexStr(String bargHexStr) {
			this.bargHexStr = bargHexStr;
		}
		
		public Long getBARGHexEmptyBytesCount() {
			return bargHexEmptyBytesCount;
		}
		public void setBARGHexEmptyBytesCount(Long bargHexEmptyBytesCount) {
			this.bargHexEmptyBytesCount = bargHexEmptyBytesCount;
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
		
		public List<SBinPlaylistObj> getPlaylistsArray() {
			return playlistsArray;
		}
		public void setPlaylistsArray(List<SBinPlaylistObj> playlistsArray) {
			this.playlistsArray = playlistsArray;
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
		@SerializedName("RepeatFieldFromNextStruct")
		private boolean repeatFieldFromNextStruct = false;
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
		
		public boolean isRepeatFieldFromNextStruct() {
			return repeatFieldFromNextStruct;
		}
		public void setRepeatFieldFromNextStruct(boolean repeatFieldFromNextStruct) {
			this.repeatFieldFromNextStruct = repeatFieldFromNextStruct;
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
		@SerializedName("OHDRUnkRemainder")
		private int ohdrUnkRemainder = 0;
		@SerializedName("HexValue")
		private String hexValue;
		@SerializedName("ExtraHexValue")
		private String extraHexValue;
		@SerializedName("StructName")
		private String structName;
		@SerializedName("StructObject")
		private boolean structObject = false;
		@SerializedName("MapElements")
		private List<String> mapElements;
		@SerializedName("StringData")
		private List<SBinStringPair> stringData;
		@SerializedName("Fields")
		private List<SBinDataField> fields;
		
		public String getOrderHexId() {
			return orderHexId;
		}
		public void setOrderHexId(String orderHexId) {
			this.orderHexId = orderHexId;
		}
		
		public int getOhdrUnkRemainder() {
			return ohdrUnkRemainder;
		}
		public void setOhdrUnkRemainder(int ohdrUnkRemainder) {
			this.ohdrUnkRemainder = ohdrUnkRemainder;
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
		
		public boolean isStructObject() {
			return structObject;
		}
		public void setStructObject(boolean structObject) {
			this.structObject = structObject;
		}
		
		public List<SBinStringPair> getStringData() {
			return stringData;
		}
		public void setStringData(List<SBinStringPair> stringData) {
			this.stringData = stringData;
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
	}
	
	public static class SBinDataField {
		@SerializedName("Name")
		private String name;
		@SerializedName("Type")
		private String type;
		@SerializedName("Enum_JsonPreview")
		private String enumJsonPreview;
		@SerializedName("EnumDataMapId_JsonPreview")
		private Long enumDataMapIdJsonPreview;
		@SerializedName("ForcedHexValue")
		private boolean forcedHexValue;
		@SerializedName("Value")
		private String value;
		
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
		
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}
	
	public static class SBinStringPair {
		@SerializedName("StringId")
		private String stringId; 
		@SerializedName("String")
		private String string; 
		@SerializedName("HalVersionValue")
		private int halVersionValue; 
		
		public String getStringId() {
			return stringId;
		}
		public void setStringId(String stringId) {
			this.stringId = stringId;
		}
		
		public String getString() {
			return string;
		}
		public void setString(String string) {
			this.string = string;
		}
		
		public int getHalVersionValue() {
			return halVersionValue;
		}
		public void setHalVersionValue(int halVersionValue) {
			this.halVersionValue = halVersionValue;
		}
	}
	
	public static class SBinPlaylistObj {
		@SerializedName("OHDRDescRemainder")
		private int ohdrDescRemainder = 0;
		@SerializedName("OHDRStruRemainder")
		private int ohdrStruRemainder = 0;
		@SerializedName("Name")
		private String name;
		@SerializedName("Playlist")
		private List<SBinPlaylistTrackObj> playlist = new ArrayList<>();
		
		public int getOhdrDescRemainder() {
			return ohdrDescRemainder;
		}
		public void setOhdrDescRemainder(int ohdrDescRemainder) {
			this.ohdrDescRemainder = ohdrDescRemainder;
		}
		
		public int getOhdrStruRemainder() {
			return ohdrStruRemainder;
		}
		public void setOhdrStruRemainder(int ohdrStruRemainder) {
			this.ohdrStruRemainder = ohdrStruRemainder;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public List<SBinPlaylistTrackObj> getPlaylist() {
			return playlist;
		}
		public void setPlaylist(List<SBinPlaylistTrackObj> playlist) {
			this.playlist = playlist;
		}
		public void addToPlaylist(SBinPlaylistTrackObj track) {
			this.playlist.add(track);
		}
	}
	
	public static class SBinPlaylistTrackObj {
		@SerializedName("OHDRUnkRemainder")
		private int ohdrUnkRemainder = 0;
		@SerializedName("FilePath")
		private String filePath;
		@SerializedName("Artist")
		private String artist;
		@SerializedName("Title")
		private String title;
		
		public int getOhdrUnkRemainder() {
			return ohdrUnkRemainder;
		}
		public void setOhdrUnkRemainder(int ohdrUnkRemainder) {
			this.ohdrUnkRemainder = ohdrUnkRemainder;
		}
		
		public String getFilePath() {
			return filePath;
		}
		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}
		
		public String getArtist() {
			return artist;
		}
		public void setArtist(String artist) {
			this.artist = artist;
		}
		
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
	}
}
