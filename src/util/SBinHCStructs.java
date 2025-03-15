package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import util.DataClasses.SBinDataElement;

public class SBinHCStructs {
	private SBinHCStructs() {}
	
	private static Map<Integer, SBinHCStruct> carConfigList = new HashMap<>();
	
	public static void initHCStructs() {
		carConfigList.putIfAbsent(0xD, new SBHCSCarConfigProperties());
	}
	
	public static SBinHCStruct getHCStruct(Integer structId) {
		SBinHCStruct hcStruct = null;
		switch(SBJson.get().getSBinType()) {
		case CAR_CONFIG:
			hcStruct = carConfigList.getOrDefault(structId, null);
			break;
		default: break;
		}
		return hcStruct != null ? hcStruct.newClass() : null;
	}
	
	//
	//
	//
	
	public static void unpackHCStructs(byte[] elementHex, SBinDataElement element, int structId) {
		switch(SBJson.get().getSBinType()) {
		case CAR_CONFIG:
			
			switch(structId) {
			case 0xD:
				unpackCarConfigPropertiesObj(elementHex, element);
				break;
			default: break;
			}
			
 			break;
		default: break;
		}
	}
	
	private static void unpackCarConfigPropertiesObj(byte[] elementHex, SBinDataElement element) {
		int headerSize = 0x4;
		int propertySize = 0xC;
		int arrayCount = 0xD;
		
		SBHCSCarConfigProperties propertiesObj = (SBHCSCarConfigProperties)element.getHCStruct();
		propertiesObj.setHeaderHex(HEXUtils.hexToString(Arrays.copyOfRange(elementHex, 0, 4)));
		int propertiesBytesTaken = propertySize * arrayCount; // No idea if header value is also about the array size
		if (headerSize + propertiesBytesTaken != elementHex.length) {
			element.setExtraHexValue(HEXUtils.hexToString(Arrays.copyOfRange(
					elementHex, headerSize + propertiesBytesTaken, elementHex.length)));
		} // Possible padding

		for (int i = 0; i < arrayCount; i++) {
			int offset = headerSize + (i * propertySize);
			byte[] entryHex = Arrays.copyOfRange(elementHex, offset, offset + propertySize);
			SBHCSCarConfigProperty entryObj = new SBHCSCarConfigProperty();

			entryObj.setPropertyNameCHDR(DataUtils.getCDATStringByShortCHDRId(entryHex, 0, 2));
			entryObj.setValueType(SBinFieldType.valueOf(
					HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(entryHex, 2, 4))));
			entryObj.setOffsetCounter(HEXUtils.hexToString(Arrays.copyOfRange(entryHex, 4, 8)));
			entryObj.setValue(entryObj.getValueType().equals(SBinFieldType.FLOAT) 
					? String.valueOf(HEXUtils.bytesToFloat(Arrays.copyOfRange(entryHex, 8, 12)))
							: HEXUtils.hexToString(Arrays.copyOfRange(entryHex, 8, 12))); // DATA Id reference or other stuff

			propertiesObj.addToProperties(entryObj);
		}
	}
	
	//
	//
	//
	
	public static class SBHCSCarConfigProperties extends SBinHCStruct {
		@SerializedName("Name")
		private String name = "CarConfigProperties";
		@SerializedName("HeaderHex")
		private String headerHex;
		@SerializedName("CarConfigProperties")
		private List<SBHCSCarConfigProperty> carConfigProperties = new ArrayList<>();
		
		@Override
		public SBinHCStruct newClass() {
			return new SBHCSCarConfigProperties();
		}
		public void addToProperties(SBHCSCarConfigProperty obj) {
			this.carConfigProperties.add(obj);
		}

		public String getHeaderHex() {
			return headerHex;
		}
		public void setHeaderHex(String headerHex) {
			this.headerHex = headerHex;
		}
		
		public List<SBHCSCarConfigProperty> getCarConfigProperties() {
			return carConfigProperties;
		}
		public void setCarConfigProperties(List<SBHCSCarConfigProperty> carConfigProperties) {
			this.carConfigProperties = carConfigProperties;
		}
	}
	
	public static class SBHCSCarConfigProperty {
		@SerializedName("PropertyNameCHDR")
		private String propertyNameCHDR;
		@SerializedName("ValueType")
		private SBinFieldType valueType;
		@SerializedName("OffsetCounter")
		private String offsetCounter;
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
		
		public String getOffsetCounter() {
			return offsetCounter;
		}
		public void setOffsetCounter(String offsetCounter) {
			this.offsetCounter = offsetCounter;
		}
		
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}
	
	public static class SBinHCStruct {
		private String name;
		@SerializedName("HCStructId")
		private int hcStructId;
		
		public SBinHCStruct newClass() {
			return new SBinHCStruct();
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
		public int getHCStructId() {
			return hcStructId;
		}
		public void setHCStructId(int hcStructId) {
			this.hcStructId = hcStructId;
		}
		
	}
}
