package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public final class DataClasses {
	private DataClasses() {}
	
	private static final byte[] SHORT_EMPTYBYTES = new byte[2];
	private static final byte[] INT_EMPTYBYTES = new byte[4];
	private static final int OHDR_MULTIPLIER = 0x8;
	
	public static class SBinBlockObj {
		private byte[] header;
		private byte[] blockSize = new byte[4];
		private int blockSizeInt = 0;
		private byte[] fnv1Hash;
		private int blockEmptyBytesCount = 0;
		private byte[] blockBytes = new byte[0];
		private List<byte[]> blockElements;
		private List<SBinOHDREntry> ohdrMapTemplate = new ArrayList<>();
		
		public void addToOHDRMapTemplate(Integer entrySize, Integer remainder) {
			this.ohdrMapTemplate.add(new SBinOHDREntry(entrySize, remainder));
		}
		
		public byte[] getHeader() {
			return header;
		}
		public void setHeader(byte[] header) {
			this.header = header;
		}
		
		public byte[] getBlockSize() {
			return blockSize;
		}
		public void setBlockSize(byte[] blockSize) {
			this.blockSize = blockSize;
		}
		
		public int getBlockSizeInt() {
			return blockSizeInt;
		}
		public void setBlockSizeInt(int blockSizeInt) {
			this.blockSizeInt = blockSizeInt;
		}
		
		public byte[] getFnv1Hash() {
			return fnv1Hash;
		}
		public void setFnv1Hash(byte[] fnv1Hash) {
			this.fnv1Hash = fnv1Hash;
		}
		
		public int getBlockEmptyBytesCount() {
			return blockEmptyBytesCount;
		}
		public void setBlockEmptyBytesCount(int blockEmptyBytesCount) {
			this.blockEmptyBytesCount = blockEmptyBytesCount;
		}
		
		public byte[] getBlockBytes() {
			return blockBytes;
		}
		public void setBlockBytes(byte[] blockBytes) {
			this.blockBytes = blockBytes;
		}
		
		public List<byte[]> getBlockElements() {
			return blockElements;
		}
		public void setBlockElements(List<byte[]> blockElements) {
			this.blockElements = blockElements;
		}

		public List<SBinOHDREntry> getOHDRMapTemplate() {
			return ohdrMapTemplate;
		}
		public void setOHDRMapTemplate(List<SBinOHDREntry> ohdrMapTemplate) {
			this.ohdrMapTemplate = ohdrMapTemplate;
		}
	}
	
	public static class SBinOHDREntry {
		private int value;
		private int remainder;
		
		public SBinOHDREntry(int value, int remainder) {
			this.value = value * OHDR_MULTIPLIER;
			this.remainder = remainder;
		}
		
		public int getValue() {
			return value;
		}
		public void setValue(int value) {
			this.value = value * OHDR_MULTIPLIER;
		}
		
		public int getRemainder() {
			return remainder;
		}
		public void setRemainder(int remainder) {
			this.remainder = remainder;
		}
	}
	
	public static class SBinStructureEntryHex {
		private byte[] header = INT_EMPTYBYTES;
		private byte[] size = INT_EMPTYBYTES;
		private List<byte[]> dataIds = new ArrayList<>();
		private byte[] padding = SHORT_EMPTYBYTES;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(this.header);
			bytes.write(this.size);
			for (byte[] obj : this.dataIds) {
				bytes.write(obj);
			}
			bytes.write(this.padding);
			return bytes.toByteArray();
		}
		
		public void addToDataIds(byte[] dataId) {
			this.dataIds.add(dataId);
		}
		
		public Integer getByteSize() {
			return 0x8 + (this.dataIds.size() * 4) + 0x2;
		}
		
		public byte[] getHeader() {
			return header;
		}
		public void setHeader(byte[] header) {
			this.header = header;
		}
		
		public byte[] getSize() {
			return size;
		}
		public void setSize(byte[] size) {
			this.size = size;
		}
		
		public List<byte[]> getDataIds() {
			return dataIds;
		}
		public void setDataIds(List<byte[]> dataIds) {
			this.dataIds = dataIds;
		}
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}
	}
	
	public static class SBinAchievementEntryHex {
		private int ohdrUnkRemainder = 0;
		private byte[] padding1 = SHORT_EMPTYBYTES;
		private byte[] name = SHORT_EMPTYBYTES;
		private byte[] desc = SHORT_EMPTYBYTES;
		private byte[] points = INT_EMPTYBYTES;
		private byte[] autologAwardId = SHORT_EMPTYBYTES;
		private byte[] padding2 = SHORT_EMPTYBYTES;
		private byte[] categoryId = INT_EMPTYBYTES;
		private byte[] metricId = INT_EMPTYBYTES;
		private byte[] metricTarget = INT_EMPTYBYTES;
		private byte[] imageName = SHORT_EMPTYBYTES;
		private byte[] imageText = SHORT_EMPTYBYTES;
		private byte[] orderId = SHORT_EMPTYBYTES;
		private byte[] padding3 = SHORT_EMPTYBYTES;
		private SBinStructureEntryHex metricMilestonesMap;
		private List<SBinAchievementMilestoneEntryHex> metricMilestones = new ArrayList<>();
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(this.padding1);
			bytes.write(this.name);
			bytes.write(this.desc);
			bytes.write(this.points);
			bytes.write(this.autologAwardId);
			bytes.write(this.padding2);
			bytes.write(this.categoryId);
			bytes.write(this.metricId);
			bytes.write(this.metricTarget);
			bytes.write(this.imageName);
			bytes.write(this.imageText);
			bytes.write(this.orderId);
			bytes.write(this.padding3);
			bytes.write(this.metricMilestonesMap.toByteArray());
			for (SBinAchievementMilestoneEntryHex obj : this.metricMilestones) {
				bytes.write(obj.toByteArray());
			}
			return bytes.toByteArray();
		}
		
		public List<SBinOHDREntry> ohdrMapTemplate() {
			List<SBinOHDREntry> ohdrEntries = new ArrayList<>();
			ohdrEntries.add(new SBinOHDREntry(0x22, this.ohdrUnkRemainder));
			ohdrEntries.add(new SBinOHDREntry(this.metricMilestonesMap.getByteSize(), 0));
			for (SBinAchievementMilestoneEntryHex obj : this.metricMilestones) {
				ohdrEntries.add(new SBinOHDREntry(obj.getByteSize(), 0));
			}
			return ohdrEntries;
		}
		
		public void addToMetricMilestones(SBinAchievementMilestoneEntryHex milestoneObj) {
			this.metricMilestones.add(milestoneObj);
		}
		
		public int getOhdrUnkRemainder() {
			return ohdrUnkRemainder;
		}
		public void setOhdrUnkRemainder(int ohdrUnkRemainder) {
			this.ohdrUnkRemainder = ohdrUnkRemainder;
		}

		public byte[] getPadding1() {
			return padding1;
		}
		public void setPadding1(byte[] padding1) {
			this.padding1 = padding1;
		}
		
		public byte[] getName() {
			return name;
		}
		public void setName(byte[] name) {
			this.name = name;
		}
		
		public byte[] getDesc() {
			return desc;
		}
		public void setDesc(byte[] desc) {
			this.desc = desc;
		}
		
		public byte[] getPoints() {
			return points;
		}
		public void setPoints(byte[] points) {
			this.points = points;
		}
		
		public byte[] getAutologAwardId() {
			return autologAwardId;
		}
		public void setAutologAwardId(byte[] autologAwardId) {
			this.autologAwardId = autologAwardId;
		}
		
		public byte[] getPadding2() {
			return padding2;
		}
		public void setPadding2(byte[] padding2) {
			this.padding2 = padding2;
		}
		
		public byte[] getCategoryId() {
			return categoryId;
		}
		public void setCategoryId(byte[] categoryId) {
			this.categoryId = categoryId;
		}
		
		public byte[] getMetricId() {
			return metricId;
		}
		public void setMetricId(byte[] metricId) {
			this.metricId = metricId;
		}
		
		public byte[] getMetricTarget() {
			return metricTarget;
		}
		public void setMetricTarget(byte[] metricTarget) {
			this.metricTarget = metricTarget;
		}
		
		public byte[] getImageName() {
			return imageName;
		}
		public void setImageName(byte[] imageName) {
			this.imageName = imageName;
		}
		
		public byte[] getImageText() {
			return imageText;
		}
		public void setImageText(byte[] imageText) {
			this.imageText = imageText;
		}
		
		public byte[] getOrderId() {
			return orderId;
		}
		public void setOrderId(byte[] orderId) {
			this.orderId = orderId;
		}
		
		public byte[] getPadding3() {
			return padding3;
		}
		public void setPadding3(byte[] padding3) {
			this.padding3 = padding3;
		}
		
		public SBinStructureEntryHex getMetricMilestonesMap() {
			return metricMilestonesMap;
		}
		public void setMetricMilestonesMap(SBinStructureEntryHex metricMilestonesMap) {
			this.metricMilestonesMap = metricMilestonesMap;
		}
		
		public List<SBinAchievementMilestoneEntryHex> getMetricMilestones() {
			return metricMilestones;
		}
		public void setMetricMilestones(List<SBinAchievementMilestoneEntryHex> metricMilestones) {
			this.metricMilestones = metricMilestones;
		}
	}
	
	public static class SBinAchievementMilestoneEntryHex {
		private byte[] header = SHORT_EMPTYBYTES;
		private byte[] intValue = INT_EMPTYBYTES;
		private byte[] padding = SHORT_EMPTYBYTES;
		
		private int byteSize = 0x8;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(this.header);
			bytes.write(this.intValue);
			bytes.write(this.padding);
			return bytes.toByteArray();
		}
		
		public Integer getByteSize() {
			return this.byteSize;
		}
		
		public byte[] getHeader() {
			return header;
		}
		public void setHeader(byte[] header) {
			this.header = header;
		}
		
		public byte[] getIntValue() {
			return intValue;
		}
		public void setIntValue(byte[] intValue) {
			this.intValue = intValue;
		}
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}
	}
	
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
		//
		@SerializedName("DATA_Elements")
		private List<SBinDataElement> dataElements;
		@SerializedName("CDAT_Strings")
		private List<SBinCDATEntry> cdatStrings;
		@SerializedName("StringData")
		private List<SBinStringDataEntry> strDataEntriesArray;
		@SerializedName("Career_FirstDATAByteValue")
		private String careerFirstDATAByteValue; 
		@SerializedName("Career_GarageCars")
		private List<String> careerGarageCarsArray;
		@SerializedName("AchievementData")
		private List<SBinAchievementEntry> achievementArray;
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
		//
		public List<SBinCDATEntry> getCDATStrings() {
			return cdatStrings;
		}
		public void setCDATStrings(List<SBinCDATEntry> cdatStrings) {
			this.cdatStrings = cdatStrings;
		}
		
		public String getCareerFirstDATAByteValue() {
			return careerFirstDATAByteValue;
		}
		public void setCareerFirstDATAByteValue(String careerFirstDATAByteValue) {
			this.careerFirstDATAByteValue = careerFirstDATAByteValue;
		}
		
		public List<String> getCareerGarageCarsArray() {
			return careerGarageCarsArray;
		}
		public void setCareerGarageCarsArray(List<String> careerGarageCarsArray) {
			this.careerGarageCarsArray = careerGarageCarsArray;
		}
		
		public List<SBinStringDataEntry> getStrDataEntriesArray() {
			return strDataEntriesArray;
		}
		public void setStrDataEntriesArray(List<SBinStringDataEntry> strDataEntriesArray) {
			this.strDataEntriesArray = strDataEntriesArray;
		}
		
		public List<SBinDataElement> getDataElements() {
			return dataElements;
		}
		public void setDataElements(List<SBinDataElement> dataElements) {
			this.dataElements = dataElements;
		}
		
		public List<SBinAchievementEntry> getAchievementArray() {
			return achievementArray;
		}
		public void setAchievementArray(List<SBinAchievementEntry> achievementArray) {
			this.achievementArray = achievementArray;
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
	
	public static class SBinStringDataEntry {
		@SerializedName("IsTextEntry")
		private boolean textEntry;
		@SerializedName("CHDRId")
		private Long chdrId;
		@SerializedName("StringId")
		private String stringId; // Entry
		@SerializedName("TextValue")
		private String textValue; // Text
		@SerializedName("CHDRIdTextRef")
		private Long chdrIdTextRef; // Entry
		@SerializedName("HalVersionValue")
		private String halVersionValue;  // Entry
		
		public boolean isTextEntry() {
			return textEntry;
		}
		public void setTextEntry(boolean textEntry) {
			this.textEntry = textEntry;
		}
		
		public Long getCHDRId() {
			return chdrId;
		}
		public void setCHDRId(Long chdrId) {
			this.chdrId = chdrId;
		}
		
		public String getStringId() {
			return stringId;
		}
		public void setStringId(String stringId) {
			this.stringId = stringId;
		}
		
		public String getTextValue() {
			return textValue;
		}
		public void setTextValue(String textValue) {
			this.textValue = textValue;
		}
		
		public Long getCHDRIdTextRef() {
			return chdrIdTextRef;
		}
		public void setCHDRIdTextRef(Long chdrIdTextRef) {
			this.chdrIdTextRef = chdrIdTextRef;
		}
		
		public String getHalVersionValue() {
			return halVersionValue;
		}
		public void setHalVersionValue(String halVersionValue) {
			this.halVersionValue = halVersionValue;
		}
	}
	
	public static class SBinDataElement {
		@SerializedName("OrderId")
		private int orderId = 0; // Only for info
		@SerializedName("OHDRUnkRemainder")
		private int ohdrUnkRemainder = 0;
		@SerializedName("HexValue")
		private String hexValue;
		
		public int getOrderId() {
			return orderId;
		}
		public void setOrderId(int orderId) {
			this.orderId = orderId;
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
	}
	
	public static class SBinAchievementEntry {
		@SerializedName("OHDRUnkRemainder")
		private int ohdrUnkRemainder = 0;
		@SerializedName("Name")
		private String name;
		@SerializedName("Desc")
		private String desc;
		@SerializedName("Points_Int")
		private int pointsInt;
		@SerializedName("AutologAwardId")
		private String autologAwardId;
		@SerializedName("CategoryId_Int")
		private int categoryId;
		@SerializedName("MetricId_Int")
		private int metricId;
		@SerializedName("MetricTarget_Int")
		private int metricTargetInt;
		@SerializedName("ImageName")
		private String imageName;
		@SerializedName("ImageText")
		private String imageText;
		@SerializedName("MetricMilestones")
		private List<Integer> metricMilestones;
		
		public int getOhdrUnkRemainder() {
			return ohdrUnkRemainder;
		}
		public void setOhdrUnkRemainder(int ohdrUnkRemainder) {
			this.ohdrUnkRemainder = ohdrUnkRemainder;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public String getDesc() {
			return desc;
		}
		public void setDesc(String desc) {
			this.desc = desc;
		}
		
		public int getPointsInt() {
			return pointsInt;
		}
		public void setPointsInt(int pointsInt) {
			this.pointsInt = pointsInt;
		}
		
		public String getAutologAwardId() {
			return autologAwardId;
		}
		public void setAutologAwardId(String autologAwardId) {
			this.autologAwardId = autologAwardId;
		}
		
		public int getCategoryId() {
			return categoryId;
		}
		public void setCategoryId(int categoryId) {
			this.categoryId = categoryId;
		}
		
		public int getMetricId() {
			return metricId;
		}
		public void setMetricId(int metricId) {
			this.metricId = metricId;
		}
		
		public int getMetricTargetInt() {
			return metricTargetInt;
		}
		public void setMetricTargetInt(int metricTargetInt) {
			this.metricTargetInt = metricTargetInt;
		}
		
		public String getImageName() {
			return imageName;
		}
		public void setImageName(String imageName) {
			this.imageName = imageName;
		}
		
		public String getImageText() {
			return imageText;
		}
		public void setImageText(String imageText) {
			this.imageText = imageText;
		}
		
		public List<Integer> getMetricMilestones() {
			return metricMilestones;
		}
		public void setMetricMilestones(List<Integer> metricMilestones) {
			this.metricMilestones = metricMilestones;
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
