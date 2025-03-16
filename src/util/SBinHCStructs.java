package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import util.DataClasses.SBinDataElement;
import util.json.JsonClassSubType;
import util.json.JsonClassType;

public class SBinHCStructs {
	private SBinHCStructs() {}
	
	private static final String HCS_TYPE_CAR_CONFIG_PROPS = "CarConfigProperties";
	private static final String HCS_TYPE_CAR_CONFIG_AXLE_CFG = "CarConfigAxleCFG";
	private static final String HCS_TYPE_CAR_CONFIG_ATTRIB = "CarConfigAttribute";
	private static final int CAR_CONFIG_HEADER_SIZE = 0x4;
	
	private static Map<Integer, SBinHCStruct> carConfigList = new HashMap<>();
	
	public static void initHCStructs() {
		carConfigList.putIfAbsent(0x2, new SBHCSCarConfigAttribute());
		carConfigList.putIfAbsent(0x6, new SBHCSCarConfigAxleCFG());
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
			case 0x2:
				unpackCarConfigAttributeObj(elementHex, element);
				break;
			case 0x6:
				unpackCarConfigAxleCFGObj(elementHex, element);
				break;
			case 0xD:
				unpackCarConfigPropertiesObj(elementHex, element);
				break;
			default: break;
			}
			
 			break;
		default: break;
		}
	}
	
	public static byte[] repackHCStructs(SBinDataElement element) throws IOException {
		byte[] hcStructBytes = null;
		switch(SBJson.get().getSBinType()) {
		case CAR_CONFIG:
			
			switch(element.getHCStruct().getHCStructId()) {
			case 0x2:
				hcStructBytes = repackCarConfigAttributeObj(element);
				break;
			case 0x6:
				hcStructBytes = repackCarConfigAxleCFGObj(element);
				break;
			case 0xD:
				hcStructBytes = repackCarConfigPropertiesObj(element);
				break;
			default: break;
			}
			
 			break;
		default: break;
		}
		if (hcStructBytes == null) {
			throw new NullPointerException("!!! HC Struct for Id " + element.getHCStruct().getHCStructId() + " is referenced, but does not exist.");
		}
		return hcStructBytes;
	}
	
	// Sometimes one Struct Id could be re-used for both SBin & Hardcoded structs in the same file
	public static boolean isExceptionForHCStructs(byte[] elementHex, int structId) {
		boolean notHCStruct = false;
		switch(SBJson.get().getSBinType()) {
		case CAR_CONFIG:
			
			switch(structId) {
			case 0x2:
				notHCStruct = structId == 0x2 && elementHex.length < 0x9;
				break;
			default: break;
			}
			
 			break;
		default: break;
		}
		return notHCStruct;
	}
	
	//
	//
	//
	
	private static void unpackCarConfigPropertiesObj(byte[] elementHex, SBinDataElement element) {
		int propertySize = 0xC;
		int arrayCount = 0xD;
		
		SBHCSCarConfigProperties propertiesObj = (SBHCSCarConfigProperties)element.getHCStruct();
		propertiesObj.setType(HCS_TYPE_CAR_CONFIG_PROPS);
		propertiesObj.setHeaderHex(HEXUtils.hexToString(Arrays.copyOfRange(elementHex, 0, 4)));
		int bytesTaken = CAR_CONFIG_HEADER_SIZE + propertySize * arrayCount; // No idea if header value is also about the array size
		checkDATAPadding(elementHex, bytesTaken, element);
		
		propertiesObj.setCarConfigProperties(readCarConfigPropertyList(CAR_CONFIG_HEADER_SIZE, bytesTaken, elementHex));
	}
	
	private static void unpackCarConfigAxleCFGObj(byte[] elementHex, SBinDataElement element) {
		SBHCSCarConfigAxleCFG axleCFGObj = (SBHCSCarConfigAxleCFG)element.getHCStruct();
		axleCFGObj.setType(HCS_TYPE_CAR_CONFIG_AXLE_CFG);
		axleCFGObj.setByteSize(HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 2, 4)));
		checkDATAPadding(elementHex, axleCFGObj.getByteSize(), element);
		
		axleCFGObj.setAxleProperties(readCarConfigPropertyList(
				CAR_CONFIG_HEADER_SIZE, axleCFGObj.getByteSize(), elementHex));
	}
	
	private static void unpackCarConfigAttributeObj(byte[] elementHex, SBinDataElement element) {
		SBHCSCarConfigAttribute attribObj = (SBHCSCarConfigAttribute)element.getHCStruct();
		attribObj.setType(HCS_TYPE_CAR_CONFIG_ATTRIB);
		attribObj.setByteSize(HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 2, 4)));
		checkDATAPadding(elementHex, attribObj.getByteSize(), element);
		
		attribObj.setAttributes(readCarConfigPropertyList(
				CAR_CONFIG_HEADER_SIZE, attribObj.getByteSize(), elementHex));
	}
	
	private static List<SBHCSCarConfigProperty> readCarConfigPropertyList(
			int headerSize, int elementRealSize, byte[] elementHex) {
		List<SBHCSCarConfigProperty> props = new ArrayList<>();
		int bytesTaken = headerSize;
		
		while (bytesTaken < elementRealSize) {
			SBHCSCarConfigProperty entryObj = new SBHCSCarConfigProperty();

			entryObj.setPropertyNameCHDR(DataUtils.getCDATStringByShortCHDRId(elementHex, bytesTaken, bytesTaken + 2));
			bytesTaken += 2;
			//
			entryObj.setValueType(SBinFieldType.valueOf(
					HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 2))));
			bytesTaken += 2;
			//
			entryObj.setOffsetCounter(HEXUtils.hexToString(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 4)));
			bytesTaken += 4;
			
			if (entryObj.getValueType().equals(SBinFieldType.FLOAT)) {
				entryObj.setValue(String.valueOf(HEXUtils.bytesToFloat(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 4))));
				bytesTaken += 4;
			} 
			else if (entryObj.getValueType().equals(SBinFieldType.BOOLEAN)) {
				int valueInt = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 4));
				entryObj.setValue(Boolean.toString(valueInt == 1));
				bytesTaken += 2;
			} 
			else {
				entryObj.setValue(HEXUtils.hexToString(Arrays.copyOfRange(elementHex, bytesTaken, bytesTaken + 4))); 
				bytesTaken += 4; // DATA Id reference or other stuff
			}
			props.add(entryObj);
		}
		
		return props;
	}
	
	private static byte[] repackCarConfigPropertiesObj(SBinDataElement element) throws IOException {
		ByteArrayOutputStream propertiesBytes = new ByteArrayOutputStream();
		SBHCSCarConfigProperties propsJson = (SBHCSCarConfigProperties)element.getHCStruct();
		propertiesBytes.write(HEXUtils.decodeHexStr(propsJson.getHeaderHex()));
		
		carConfigPropertyToBytes(propsJson.getCarConfigProperties(), propertiesBytes);
		return propertiesBytes.toByteArray();
	}
	
	private static byte[] repackCarConfigAxleCFGObj(SBinDataElement element) throws IOException {
		ByteArrayOutputStream axleCFGBytes = new ByteArrayOutputStream();
		SBHCSCarConfigAxleCFG axleCFGJson = (SBHCSCarConfigAxleCFG)element.getHCStruct();
		axleCFGBytes.write(HEXUtils.shortToBytes(axleCFGJson.getHCStructId()));
		axleCFGBytes.write(HEXUtils.shortToBytes(axleCFGJson.getByteSize()));
		
		carConfigPropertyToBytes(axleCFGJson.getAxleProperties(), axleCFGBytes);
		return axleCFGBytes.toByteArray();
	}
	
	private static byte[] repackCarConfigAttributeObj(SBinDataElement element) throws IOException {
		ByteArrayOutputStream attribBytes = new ByteArrayOutputStream();
		SBHCSCarConfigAttribute attribJson = (SBHCSCarConfigAttribute)element.getHCStruct();
		attribBytes.write(HEXUtils.shortToBytes(attribJson.getHCStructId()));
		attribBytes.write(HEXUtils.shortToBytes(attribJson.getByteSize()));
		
		carConfigPropertyToBytes(attribJson.getAttributes(), attribBytes);
		return attribBytes.toByteArray();
	}
	
	private static void carConfigPropertyToBytes(List<SBHCSCarConfigProperty> props, ByteArrayOutputStream propertiesBytes) throws IOException {
		for (SBHCSCarConfigProperty property : props) {
			propertiesBytes.write(DataUtils.processStringInCDAT(property.getPropertyNameCHDR()));
			propertiesBytes.write(HEXUtils.shortToBytes(property.getValueType().getId()));
			propertiesBytes.write(HEXUtils.decodeHexStr(property.getOffsetCounter()));
			
			if (property.getValueType().equals(SBinFieldType.FLOAT)) {
				propertiesBytes.write(HEXUtils.floatToBytes(Float.parseFloat(property.getValue())));
			} 
			else if (property.getValueType().equals(SBinFieldType.BOOLEAN)) {
				int bool = Boolean.parseBoolean(property.getValue()) ? 0x1 : 0x0;
				propertiesBytes.write(new byte[] {(byte)bool, 0x0});
			} 
			else {
				propertiesBytes.write(HEXUtils.decodeHexStr(property.getValue()));
			}
		}
	}
	
	//
	//
	//
	
	private static void checkDATAPadding(byte[] elementHex, int bytesTaken, SBinDataElement element) {
		if (bytesTaken != elementHex.length) {
			element.setExtraHexValue(HEXUtils.hexToString(Arrays.copyOfRange(
					elementHex, bytesTaken, elementHex.length)));
		}
	}
	
	//
	//
	//
	
	public static class SBHCSCarConfigAttribute extends SBinHCStruct {
		@SerializedName("ByteSize")
		private int byteSize;
		@SerializedName("CarConfigAttribute")
		private List<SBHCSCarConfigProperty> attributes;
		
		@Override
		public SBinHCStruct newClass() {
			return new SBHCSCarConfigAttribute();
		}

		public int getByteSize() {
			return byteSize;
		}
		public void setByteSize(int byteSize) {
			this.byteSize = byteSize;
		}

		public List<SBHCSCarConfigProperty> getAttributes() {
			return attributes;
		}
		public void setAttributes(List<SBHCSCarConfigProperty> attributes) {
			this.attributes = attributes;
		}
	}
	
	public static class SBHCSCarConfigAxleCFG extends SBinHCStruct {
		@SerializedName("ByteSize")
		private int byteSize;
		@SerializedName("AxleProperties")
		private List<SBHCSCarConfigProperty> axleProperties;
		
		@Override
		public SBinHCStruct newClass() {
			return new SBHCSCarConfigAxleCFG();
		}
		
		public int getByteSize() {
			return byteSize;
		}
		public void setByteSize(int byteSize) {
			this.byteSize = byteSize;
		}

		public List<SBHCSCarConfigProperty> getAxleProperties() {
			return axleProperties;
		}
		public void setAxleProperties(List<SBHCSCarConfigProperty> axleProperties) {
			this.axleProperties = axleProperties;
		}
	}
	
	public static class SBHCSCarConfigProperties extends SBinHCStruct {
		@SerializedName("HeaderHex")
		private String headerHex;
		@SerializedName("CarConfigProperties")
		private List<SBHCSCarConfigProperty> carConfigProperties;
		
		@Override
		public SBinHCStruct newClass() {
			return new SBHCSCarConfigProperties();
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
	
	@JsonClassType(property = "Type",
		subTypes = {
			@JsonClassSubType(jsonClass = SBHCSCarConfigProperties.class, name = HCS_TYPE_CAR_CONFIG_PROPS),
			@JsonClassSubType(jsonClass = SBHCSCarConfigAxleCFG.class, name = HCS_TYPE_CAR_CONFIG_AXLE_CFG),
			@JsonClassSubType(jsonClass = SBHCSCarConfigAttribute.class, name = HCS_TYPE_CAR_CONFIG_ATTRIB)
		}
	)
	public static class SBinHCStruct {
		@SerializedName("Type")
		private String type;
		@SerializedName("HCStructId")
		private int hcStructId;
		
		public SBinHCStruct newClass() {
			return new SBinHCStruct();
		}
		
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		
		public int getHCStructId() {
			return hcStructId;
		}
		public void setHCStructId(int hcStructId) {
			this.hcStructId = hcStructId;
		}
		
	}
}
