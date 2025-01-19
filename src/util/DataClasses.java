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
		private int enumHexEmptyBytesCount; 
		@SerializedName("STRU_HexStr")
		private String struHexStr; 
		@SerializedName("STRU_HexEmptyBytesCount")
		private int struHexEmptyBytesCount; 
		@SerializedName("FIEL_HexStr")
		private String fielHexStr; 
		@SerializedName("FIEL_HexEmptyBytesCount")
		private int fielHexEmptyBytesCount; 
		@SerializedName("OHDR_HexStr")
		private String ohdrHexStr; 
		@SerializedName("OHDR_HexEmptyBytesCount")
		private int ohdrHexEmptyBytesCount; 
		@SerializedName("DATA_HexStr")
		private String dataHexStr; 
		@SerializedName("DATA_HexEmptyBytesCount")
		private int dataHexEmptyBytesCount; 
		@SerializedName("CHDR_HexStr")
		private String chdrHexStr; 
		@SerializedName("CHDR_HexEmptyBytesCount")
		private int chdrHexEmptyBytesCount; 
		@SerializedName("CDAT_HexStr")
		private String cdatHexStr;
		@SerializedName("CDAT_HexEmptyBytesCount")
		private int cdatHexEmptyBytesCount;
		@SerializedName("CDAT_Strings")
		private List<String> cdatStrings;
		//
		@SerializedName("StringData_Text")
		private List<SBinStringDataText> strDataTextArray;
		@SerializedName("StringData_Entries")
		private List<SBinStringDataEntry> strDataEntriesArray;
		
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
		
		public int getENUMHexEmptyBytesCount() {
			return enumHexEmptyBytesCount;
		}
		public void setENUMHexEmptyBytesCount(int enumHexEmptyBytesCount) {
			this.enumHexEmptyBytesCount = enumHexEmptyBytesCount;
		}
		
		public String getSTRUHexStr() {
			return struHexStr;
		}
		public void setSTRUHexStr(String struHexStr) {
			this.struHexStr = struHexStr;
		}
		
		public int getSTRUHexEmptyBytesCount() {
			return struHexEmptyBytesCount;
		}
		public void setSTRUHexEmptyBytesCount(int struHexEmptyBytesCount) {
			this.struHexEmptyBytesCount = struHexEmptyBytesCount;
		}
		
		public String getFIELHexStr() {
			return fielHexStr;
		}
		public void setFIELHexStr(String fielHexStr) {
			this.fielHexStr = fielHexStr;
		}
		
		public int getFIELHexEmptyBytesCount() {
			return fielHexEmptyBytesCount;
		}
		public void setFIELHexEmptyBytesCount(int fielHexEmptyBytesCount) {
			this.fielHexEmptyBytesCount = fielHexEmptyBytesCount;
		}
		
		public String getOHDRHexStr() {
			return ohdrHexStr;
		}
		public void setOHDRHexStr(String ohdrHexStr) {
			this.ohdrHexStr = ohdrHexStr;
		}
		
		public int getOHDRHexEmptyBytesCount() {
			return ohdrHexEmptyBytesCount;
		}
		public void setOHDRHexEmptyBytesCount(int ohdrHexEmptyBytesCount) {
			this.ohdrHexEmptyBytesCount = ohdrHexEmptyBytesCount;
		}
		
		public String getDATAHexStr() {
			return dataHexStr;
		}
		public void setDATAHexStr(String dataHexStr) {
			this.dataHexStr = dataHexStr;
		}
		
		public int getDATAHexEmptyBytesCount() {
			return dataHexEmptyBytesCount;
		}
		public void setDATAHexEmptyBytesCount(int dataHexEmptyBytesCount) {
			this.dataHexEmptyBytesCount = dataHexEmptyBytesCount;
		}
		
		public String getCHDRHexStr() {
			return chdrHexStr;
		}
		public void setCHDRHexStr(String chdrHexStr) {
			this.chdrHexStr = chdrHexStr;
		}
		
		public int getCHDRHexEmptyBytesCount() {
			return chdrHexEmptyBytesCount;
		}
		public void setCHDRHexEmptyBytesCount(int chdrHexEmptyBytesCount) {
			this.chdrHexEmptyBytesCount = chdrHexEmptyBytesCount;
		}
		
		public String getCDATHexStr() {
			return cdatHexStr;
		}
		public void setCDATHexStr(String cdatHexStr) {
			this.cdatHexStr = cdatHexStr;
		}
		
		public int getCDATHexEmptyBytesCount() {
			return cdatHexEmptyBytesCount;
		}
		public void setCDATHexEmptyBytesCount(int cdatHexEmptyBytesCount) {
			this.cdatHexEmptyBytesCount = cdatHexEmptyBytesCount;
		}
		
		public List<String> getCDATStrings() {
			return cdatStrings;
		}
		public void setCDATStrings(List<String> cdatStrings) {
			this.cdatStrings = cdatStrings;
		}
		
		public List<SBinStringDataText> getStrDataTextArray() {
			return strDataTextArray;
		}
		public void setStrDataTextArray(List<SBinStringDataText> strDataTextArray) {
			this.strDataTextArray = strDataTextArray;
		}
		
		public List<SBinStringDataEntry> getStrDataEntriesArray() {
			return strDataEntriesArray;
		}
		public void setStrDataEntriesArray(List<SBinStringDataEntry> strDataEntriesArray) {
			this.strDataEntriesArray = strDataEntriesArray;
		}
	}
	
	public static class SBinStringDataText {
		@SerializedName("TextId")
		private int textId; 
		@SerializedName("Text")
		private String text;
		
		public int getTextId() {
			return textId;
		}
		public void setTextId(int textId) {
			this.textId = textId;
		}
		
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		} 
	}
	
	public static class SBinStringDataEntry {
		@SerializedName("StringId")
		private String stringId; 
		@SerializedName("TextId")
		private int textId;
		@SerializedName("UnkValue")
		private String unkValue; 
		//
		private SBinStringDataText textObj;
		
		public String getStringId() {
			return stringId;
		}
		public void setStringId(String stringId) {
			this.stringId = stringId;
		}
		
		public int getTextId() {
			return textId;
		}
		public void setTextId(int textId) {
			this.textId = textId;
		}
		
		public String getUnkValue() {
			return unkValue;
		}
		public void setUnkValue(String unkValue) {
			this.unkValue = unkValue;
		}
		
		public SBinStringDataText getTextObj() {
			return textObj;
		}
		public void setTextObj(SBinStringDataText textObj) {
			this.textObj = textObj;
		}
	}
}
