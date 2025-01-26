package util;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public final class DataClasses {
	private DataClasses() {}
	
	public static class SBinBlockObj {
		private byte[] header;
		private byte[] blockSize = new byte[4];
		private int blockSizeInt = 0;
		private byte[] fnv1Hash;
		private int blockEmptyBytesCount = 0;
		private byte[] blockBytes = new byte[0];
		private List<byte[]> blockElements;
		
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
	}
	
	public static class SBinJson {
		@SerializedName("FileName")
		private String fileName; 
		@SerializedName("SBinVersion")
		private int sbinVersion; 
		@SerializedName("SBinType")
		private String sbinType; 
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
		@SerializedName("CDAT_Strings")
		private List<String> cdatStrings;
		@SerializedName("StringData")
		private List<SBinStringDataEntry> strDataEntriesArray;
		@SerializedName("Career_FirstDATAByteValue")
		private String careerFirstDATAByteValue; 
		@SerializedName("Career_GarageCars")
		private List<String> careerGarageCarsArray;
		private SBinType sbinTypeEnum;
		
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
		
		public String getSBinType() {
			return sbinType;
		}
		public void setSBinType(String sbinType) {
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
		public List<String> getCDATStrings() {
			return cdatStrings;
		}
		public void setCDATStrings(List<String> cdatStrings) {
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
		
		public SBinType getSBinTypeEnum() {
			return sbinTypeEnum;
		}
		public void setSBinTypeEnum(SBinType sbinTypeEnum) {
			this.sbinTypeEnum = sbinTypeEnum;
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
}
