package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class HEXClasses {
	private HEXClasses() {}
	
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
		private boolean lastBlock = false;
		private List<SBinOHDREntry> ohdrMapTemplate = new ArrayList<>();
		private byte[] bulkMap;
		
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

		public boolean isLastBlock() {
			return lastBlock;
		}
		public void setLastBlock(boolean lastBlock) {
			this.lastBlock = lastBlock;
		}

		public List<SBinOHDREntry> getOHDRMapTemplate() {
			return ohdrMapTemplate;
		}
		public void setOHDRMapTemplate(List<SBinOHDREntry> ohdrMapTemplate) {
			this.ohdrMapTemplate = ohdrMapTemplate;
		}

		public byte[] getBULKMap() {
			return bulkMap;
		}
		public void setBULKMap(byte[] bulkMap) {
			this.bulkMap = bulkMap;
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
			return 0x8 + (this.dataIds.size() * 4) + this.padding.length;
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
	
	public static class SBinStructHex {
		private byte[] nameCHDRRef = SHORT_EMPTYBYTES;
		private byte[] firstFieldOffset = SHORT_EMPTYBYTES;
		private byte[] fieldCount = SHORT_EMPTYBYTES;
		private List<SBinFieldHex> fields = new ArrayList<>();
		
		public void addToFields(SBinFieldHex field) {
			this.fields.add(field);
		}
		
		public byte[] getNameCHDRRef() {
			return nameCHDRRef;
		}
		public void setNameCHDRRef(byte[] nameCHDRRef) {
			this.nameCHDRRef = nameCHDRRef;
		}
		
		public byte[] getFirstFieldOffset() {
			return firstFieldOffset;
		}
		public void setFirstFieldOffset(byte[] firstFieldOffset) {
			this.firstFieldOffset = firstFieldOffset;
		}
		
		public byte[] getFieldCount() {
			return fieldCount;
		}
		public void setFieldCount(byte[] fieldCount) {
			this.fieldCount = fieldCount;
		}

		public List<SBinFieldHex> getFields() {
			return fields;
		}
		public void setFields(List<SBinFieldHex> fields) {
			this.fields = fields;
		}
	}
	
	public static class SBinFieldHex {
		private byte[] nameCHDRRef = SHORT_EMPTYBYTES;
		private byte[] type = SHORT_EMPTYBYTES;
		private byte[] startOffset = SHORT_EMPTYBYTES;
		private byte[] unkOrderId = SHORT_EMPTYBYTES;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(this.nameCHDRRef);
			bytes.write(this.type);
			bytes.write(this.startOffset);
			bytes.write(this.unkOrderId);
			return bytes.toByteArray();
		}
		
		public byte[] getNameCHDRRef() {
			return nameCHDRRef;
		}
		public void setNameCHDRRef(byte[] nameCHDRRef) {
			this.nameCHDRRef = nameCHDRRef;
		}
		
		public byte[] getType() {
			return type;
		}
		public void setType(byte[] type) {
			this.type = type;
		}
		
		public byte[] getStartOffset() {
			return startOffset;
		}
		public void setStartOffset(byte[] startOffset) {
			this.startOffset = startOffset;
		}
		
		public byte[] getUnkOrderId() {
			return unkOrderId;
		}
		public void setUnkOrderId(byte[] unkOrderId) {
			this.unkOrderId = unkOrderId;
		}
	}
	
	public static class ETC1PKMTexture {
		private byte[] header = HEXUtils.stringToBytes("PKM 10");
		private byte[] type = new byte[2]; // ETC1_RGB_NO_MIPMAPS
		private byte[] encWidth;
		private byte[] encHeight;
		private byte[] width;
		private byte[] height;
		private byte[] imageData;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(this.header);
			bytes.write(this.type);
			bytes.write(this.encWidth);
			bytes.write(this.encHeight);
			bytes.write(this.width);
			bytes.write(this.height);
			bytes.write(this.imageData);
			return bytes.toByteArray();
		}
		
		public byte[] getHeader() {
			return header;
		}
		public void setHeader(byte[] header) {
			this.header = header;
		}
		
		public byte[] getType() {
			return type;
		}
		public void setType(byte[] type) {
			this.type = type;
		}
		
		public byte[] getEncWidth() {
			return encWidth;
		}
		public void setEncWidth(byte[] encWidth) {
			this.encWidth = encWidth;
		}
		
		public byte[] getEncHeight() {
			return encHeight;
		}
		public void setEncHeight(byte[] encHeight) {
			this.encHeight = encHeight;
		}
		
		public byte[] getWidth() {
			return width;
		}
		public void setWidth(byte[] width) {
			this.width = width;
		}
		
		public byte[] getHeight() {
			return height;
		}
		public void setHeight(byte[] height) {
			this.height = height;
		}
		
		public byte[] getImageData() {
			return imageData;
		}
		public void setImageData(byte[] imageData) {
			this.imageData = imageData;
		}
	}
	
	//
	// IM2M3G
	//
	
	public static class M3GModel {
		private byte[] header;
		private byte isCompressed;
		private byte[] fileSize;
		private byte[] uncompressedFileSize;
		private List<M3GObject> objectArray = new ArrayList<>();
		private byte[] checksum;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			for (M3GObject obj : objectArray) {
				bytes.write(obj.toByteArray());
			}
			bytes.write(this.checksum);
			
			byte[] newFileSize = HEXUtils.intToByteArrayLE(bytes.size() + 0x9); // compress byte + file sizes
			ByteArrayOutputStream finalBytes = new ByteArrayOutputStream();
			finalBytes.write(this.header);
			finalBytes.write(this.isCompressed);
			finalBytes.write(newFileSize); // fileSize
			finalBytes.write(newFileSize); // uncompressedFileSize
			finalBytes.write(bytes.toByteArray());
			return finalBytes.toByteArray();
		}
		
		public void addObject(M3GObject obj) {
			this.objectArray.add(obj);
		}

		public byte[] getHeader() {
			return header;
		}
		public void setHeader(byte[] header) {
			this.header = header;
		}

		public byte getIsCompressed() {
			return isCompressed;
		}
		public void setIsCompressed(byte isCompressed) {
			this.isCompressed = isCompressed;
		}

		public byte[] getFileSize() {
			return fileSize;
		}
		public void setFileSize(byte[] fileSize) {
			this.fileSize = fileSize;
		}

		public byte[] getUncompressedFileSize() {
			return uncompressedFileSize;
		}
		public void setUncompressedFileSize(byte[] uncompressedFileSize) {
			this.uncompressedFileSize = uncompressedFileSize;
		}

		public List<M3GObject> getObjectArray() {
			return objectArray;
		}
		public void setObjectArray(List<M3GObject> objectArray) {
			this.objectArray = objectArray;
		}

		public byte[] getChecksum() {
			return checksum;
		}
		public void setChecksum(byte[] checksum) {
			this.checksum = checksum;
		}
	}
	
	public static class M3GObject {
		private int startAddr;
		private int type; // byte
		private int size;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			return bytes.toByteArray();
		}
		
		public int getStartAddr() {
			return startAddr;
		}
		public void setStartAddr(int startAddr) {
			this.startAddr = startAddr;
		}

		public int getType() {
			return type;
		}
		public byte getTypeByte() {
			return (byte)type;
		}
		public void setType(byte type) {
			this.type = (int)type;
		}
		
		public int getSize() {
			return size;
		}
		public byte[] getSizeBytes() {
			return HEXUtils.intToByteArrayLE(size);
		}
		public void setSize(byte[] size) {
			this.size = HEXUtils.byteArrayToInt(size);
		}
	}
	
	public static class M3GObjGeneric extends M3GObject {
		private byte[] data;
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.data);
			return bytes.toByteArray();
		}

		public byte[] getData() {
			return data;
		}
		public void setData(byte[] data) {
			this.data = data;
		}
	}
	
	public static class M3GSubObjParameterProperty {
		private int objLabelOffset; // byte
		private String objLabel;
		private int propertyLabelOffset;
		private String property;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getObjLabelOffsetByte());
			bytes.write(getObjLabelBytes());
			bytes.write(getPropertyLabelOffsetBytes());
			bytes.write(getPropertyBytes());
			return bytes.toByteArray();
		}
		
		public int getObjLabelOffset() {
			return objLabelOffset;
		}
		public byte getObjLabelOffsetByte() {
			return (byte)objLabelOffset;
		}
		public void setObjLabelOffset(byte objLabelOffset) {
			this.objLabelOffset = (int)objLabelOffset;
		}
		
		public String getObjLabel() {
			return objLabel;
		}
		public byte[] getObjLabelBytes() {
			return HEXUtils.stringToBytes(objLabel);
		}
		public void setObjLabel(String objLabel) {
			this.objLabel = objLabel;
		}
		
		public int getPropertyLabelOffset() {
			return propertyLabelOffset;
		}
		public byte[] getPropertyLabelOffsetBytes() {
			return HEXUtils.intToByteArrayLE(propertyLabelOffset);
		}
		public void setPropertyLabelOffset(byte[] propertyLabelOffset) {
			this.propertyLabelOffset = HEXUtils.byteArrayToInt(propertyLabelOffset);
		}
		
		public String getProperty() {
			return property;
		}
		public byte[] getPropertyBytes() {
			return HEXUtils.stringToBytes(property);
		}
		public void setProperty(String property) {
			this.property = property;
		}
	}
	
	public static class M3GSubObjParameter {
		private int type;
		private int size;
		private String strValue; // type 0x0
		private float[] rgbaColorValue = new float[4]; // type 0x259
		private M3GSubObjParameterProperty propertyObj; // type 0x2
		private byte[] unkBytes;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeBytes());
			bytes.write(getSizeBytes());
			
			switch(this.type) {
			case 0x0: case 0x384:
				bytes.write(getStrValueBytes());
				break;
			case 0x2:
				bytes.write(this.propertyObj.toByteArray());
				break;
			case 0x259:
				bytes.write(HEXUtils.floatToBytes(this.rgbaColorValue[0]));
				bytes.write(HEXUtils.floatToBytes(this.rgbaColorValue[1]));
				bytes.write(HEXUtils.floatToBytes(this.rgbaColorValue[2]));
				bytes.write(HEXUtils.floatToBytes(this.rgbaColorValue[3]));
				break;
			default:
				bytes.write(this.unkBytes);
				break;
			}
			return bytes.toByteArray();
		}
		
		public int getType() {
			return type;
		}
		public byte[] getTypeBytes() {
			return HEXUtils.intToByteArrayLE(type);
		}
		public void setType(byte[] type) {
			this.type = HEXUtils.byteArrayToInt(type);
		}
		
		public int getSize() {
			return size;
		}
		public byte[] getSizeBytes() {
			return HEXUtils.intToByteArrayLE(size);
		}
		public void setSize(byte[] size) {
			this.size = HEXUtils.byteArrayToInt(size);
		}
		
		public String getStrValue() {
			return strValue;
		}
		public byte[] getStrValueBytes() {
			return HEXUtils.stringToBytes(strValue);
		}
		public void setStrValue(String strValue) {
			this.strValue = strValue;
		}
		
		public float[] getRGBAColorValue() {
			return rgbaColorValue;
		}
		public void setRGBAColorValue(float[] rgbaColorValue) {
			this.rgbaColorValue = rgbaColorValue;
		}
		
		public M3GSubObjParameterProperty getPropertyObj() {
			return propertyObj;
		}
		public void setPropertyObj(M3GSubObjParameterProperty propertyObj) {
			this.propertyObj = propertyObj;
		}
		
		public byte[] getUnkBytes() {
			return unkBytes;
		}
		public void setUnkBytes(byte[] unkBytes) {
			this.unkBytes = unkBytes;
		}
	}
	
	public static class M3GSubObjComponentTransform {
		private float[] translation = new float[3];
		private float[] scale = new float[3];
		private float orientationAngle;
		private float orientationAxisX;
		private float orientationAxisY;
		private float orientationAxisZ;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(HEXUtils.floatToBytes(this.translation[0]));
			bytes.write(HEXUtils.floatToBytes(this.translation[1]));
			bytes.write(HEXUtils.floatToBytes(this.translation[2]));
			bytes.write(HEXUtils.floatToBytes(this.scale[0]));
			bytes.write(HEXUtils.floatToBytes(this.scale[1]));
			bytes.write(HEXUtils.floatToBytes(this.scale[2]));
			bytes.write(getOrientationAngleBytes());
			bytes.write(getOrientationAxisXBytes());
			bytes.write(getOrientationAxisYBytes());
			bytes.write(getOrientationAxisZBytes());
			return bytes.toByteArray();
		}
		
		public float[] getTranslation() {
			return translation;
		}
		public void setTranslation(float[] translation) {
			this.translation = translation;
		}
		
		public float[] getScale() {
			return scale;
		}
		public void setScale(float[] scale) {
			this.scale = scale;
		}
		
		public float getOrientationAngle() {
			return orientationAngle;
		}
		public byte[] getOrientationAngleBytes() {
			return HEXUtils.floatToBytes(orientationAngle);
		}
		public void setOrientationAngle(byte[] orientationAngle) {
			this.orientationAngle = HEXUtils.bytesToFloat(orientationAngle);
		}
		
		public float getOrientationAxisX() {
			return orientationAxisX;
		}
		public byte[] getOrientationAxisXBytes() {
			return HEXUtils.floatToBytes(orientationAxisX);
		}
		public void setOrientationAxisX(byte[] orientationAxisX) {
			this.orientationAxisX = HEXUtils.bytesToFloat(orientationAxisX);
		}
		
		public float getOrientationAxisY() {
			return orientationAxisY;
		}
		public byte[] getOrientationAxisYBytes() {
			return HEXUtils.floatToBytes(orientationAxisY);
		}
		public void setOrientationAxisY(byte[] orientationAxisY) {
			this.orientationAxisY = HEXUtils.bytesToFloat(orientationAxisY);
		}
		
		public float getOrientationAxisZ() {
			return orientationAxisZ;
		}
		public byte[] getOrientationAxisZBytes() {
			return HEXUtils.floatToBytes(orientationAxisZ);
		}
		public void setOrientationAxisZ(byte[] orientationAxisZ) {
			this.orientationAxisZ = HEXUtils.bytesToFloat(orientationAxisZ);
		}
	}
	
	public static class M3GSubObjTextureCoord {
		private int textureCoordObjIndex;
		private float[] textureCoordBias = new float[3];
		private float textureCoordScale;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTextureCoordObjIndexBytes());
			bytes.write(HEXUtils.floatToBytes(this.textureCoordBias[0]));
			bytes.write(HEXUtils.floatToBytes(this.textureCoordBias[1]));
			bytes.write(HEXUtils.floatToBytes(this.textureCoordBias[2]));
			bytes.write(getTextureCoordScaleBytes());
			return bytes.toByteArray();
		}
		
		public int getTextureCoordObjIndex() {
			return textureCoordObjIndex;
		}
		public byte[] getTextureCoordObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(textureCoordObjIndex);
		}
		public void setTextureCoordObjIndex(byte[] textureCoordObjIndex) {
			this.textureCoordObjIndex = HEXUtils.byteArrayToInt(textureCoordObjIndex);
		}
		public void setTextureCoordObjIndexInc(int increaseIds) {
			if (this.textureCoordObjIndex > 0) {
				this.textureCoordObjIndex = this.textureCoordObjIndex + increaseIds;
			}
		}
		
		public float[] getTextureCoordBias() {
			return textureCoordBias;
		}
		public void setTextureCoordBias(float[] textureCoordBias) {
			this.textureCoordBias = textureCoordBias;
		}
		
		public float getTextureCoordScale() {
			return textureCoordScale;
		}
		public byte[] getTextureCoordScaleBytes() {
			return HEXUtils.floatToBytes(textureCoordScale);
		}
		public void setTextureCoordScale(byte[] textureCoordScale) {
			this.textureCoordScale = HEXUtils.bytesToFloat(textureCoordScale);
		}
	}
	
	// 0x1
	public static class M3GObjAnimationTrackSettings extends M3GObject {
		private byte[] padding = new byte[12];
		private float value1;
		private float value2;
		private byte[] unkPart = new byte[16];
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.padding);
			bytes.write(getValue1Bytes());
			bytes.write(getValue2Bytes());
			bytes.write(this.unkPart);
			return bytes.toByteArray();
		}

		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}

		public float getValue1() {
			return value1;
		}
		public byte[] getValue1Bytes() {
			return HEXUtils.floatToBytes(value1);
		}
		public void setValue1(byte[] value1) {
			this.value1 = HEXUtils.bytesToFloat(value1);
		}

		public float getValue2() {
			return value2;
		}
		public byte[] getValue2Bytes() {
			return HEXUtils.floatToBytes(value2);
		}
		public void setValue2(byte[] value2) {
			this.value2 = HEXUtils.bytesToFloat(value2);
		}

		public byte[] getUnkPart() {
			return unkPart;
		}
		public void setUnkPart(byte[] unkPart) {
			this.unkPart = unkPart;
		}
	}
	
	// 0x2
	public static class M3GObjAnimationTrack extends M3GObject {
		private byte[] padding = new byte[12];
		private int animationBufferObjIndex;
		private int animationTrackSettingsObjIndex;
		private byte[] unkPart = new byte[4];
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.padding);
			bytes.write(getAnimationBufferObjIndexBytes());
			bytes.write(getAnimationTrackSettingsObjIndexBytes());
			bytes.write(this.unkPart);
			return bytes.toByteArray();
		}

		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}

		public int getAnimationBufferObjIndex() {
			return animationBufferObjIndex;
		}
		public byte[] getAnimationBufferObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(animationBufferObjIndex);
		}
		public void setAnimationBufferObjIndex(byte[] animationBufferObjIndex) {
			this.animationBufferObjIndex = HEXUtils.byteArrayToInt(animationBufferObjIndex);
		}
		public void setAnimationBufferObjIndexInc(int increaseIds) {
			if (this.animationBufferObjIndex > 0) {
				this.animationBufferObjIndex = this.animationBufferObjIndex + increaseIds;
			}
		}

		public int getAnimationTrackSettingsObjIndex() {
			return animationTrackSettingsObjIndex;
		}
		public byte[] getAnimationTrackSettingsObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(animationTrackSettingsObjIndex);
		}
		public void setAnimationTrackSettingsObjIndex(byte[] animationTrackSettingsObjIndex) {
			this.animationTrackSettingsObjIndex = HEXUtils.byteArrayToInt(animationTrackSettingsObjIndex);
		}
		public void setAnimationTrackSettingsObjIndexInc(int increaseIds) {
			if (this.animationTrackSettingsObjIndex > 0) {
				this.animationTrackSettingsObjIndex = this.animationTrackSettingsObjIndex + increaseIds;
			}
		}
		
		public byte[] getUnkPart() {
			return unkPart;
		}
		public void setUnkPart(byte[] unkPart) {
			this.unkPart = unkPart;
		}
	}
	
	// 0x3
	public static class M3GObjAppearance extends M3GObject {
		private byte[] animationControllers;
		private int animationTracks;
		private List<Integer> animationArray = new ArrayList<>(); // ???
		private int parameterCount;
		private List<M3GSubObjParameter> parameterArray = new ArrayList<>();
		private int layer; // byte
		private int compositingModeObjIndex;
		private int fogObjIndex;
		private int polygonModeObjIndex;
		private int materialObjIndex;
		private int textureRefCount;
		private List<Integer> textureRefObjIndexArray = new ArrayList<>();
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.animationControllers);
			bytes.write(getAnimationTracksBytes());
			for (Integer id : getAnimationArray()) {
				bytes.write(HEXUtils.intToByteArrayLE(id));
			}
			bytes.write(getParameterCountBytes());
			for (M3GSubObjParameter paramObj : getParameterArray()) {
				bytes.write(paramObj.toByteArray());
			}
			bytes.write(getLayerByte());
			bytes.write(getCompositingModeObjIndexBytes());
			bytes.write(getFogObjIndexBytes());
			bytes.write(getPolygonModeObjIndexBytes());
			bytes.write(getMaterialObjIndexBytes());
			bytes.write(getTextureRefCountBytes());
			for (Integer id2 : getTextureRefObjIndexArray()) {
				bytes.write(HEXUtils.intToByteArrayLE(id2));
			}
			return bytes.toByteArray();
		}
		
		public byte[] getAnimationControllers() {
			return animationControllers;
		}
		public void setAnimationControllers(byte[] animationControllers) {
			this.animationControllers = animationControllers;
		}
		
		public int getAnimationTracks() {
			return animationTracks;
		}
		public byte[] getAnimationTracksBytes() {
			return HEXUtils.intToByteArrayLE(animationTracks);
		}
		public void setAnimationTracks(byte[] animationTracks) {
			this.animationTracks = HEXUtils.byteArrayToInt(animationTracks);
		}
		
		public void addToAnimationArray(int objId) {
			this.animationArray.add(objId);
		}
		public List<Integer> getAnimationArray() {
			return animationArray;
		}
		public void setAnimationArray(List<Integer> animationArray) {
			this.animationArray = animationArray;
		}
		
		public int getParameterCount() {
			return parameterCount;
		}
		public byte[] getParameterCountBytes() {
			return HEXUtils.intToByteArrayLE(parameterCount);
		}
		public void setParameterCount(byte[] parameterCount) {
			this.parameterCount = HEXUtils.byteArrayToInt(parameterCount);
		}
		
		public void addToParameterArray(M3GSubObjParameter obj) {
			this.parameterArray.add(obj);
		}
		public List<M3GSubObjParameter> getParameterArray() {
			return parameterArray;
		}
		public void setParameterArray(List<M3GSubObjParameter> parameterArray) {
			this.parameterArray = parameterArray;
		}
		
		public int getLayer() {
			return layer;
		}
		public byte getLayerByte() {
			return (byte)layer;
		}
		public void setLayer(byte layer) {
			this.layer = (int)layer;
		}
		
		public int getCompositingModeObjIndex() {
			return compositingModeObjIndex;
		}
		public byte[] getCompositingModeObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(compositingModeObjIndex);
		}
		public void setCompositingModeObjIndex(byte[] compositingModeObjIndex) {
			this.compositingModeObjIndex = HEXUtils.byteArrayToInt(compositingModeObjIndex);
		}
		public void setCompositingModeObjIndexInc(int increaseIds) {
			if (this.compositingModeObjIndex > 0) {
				this.compositingModeObjIndex = this.compositingModeObjIndex + increaseIds;
			}
		}
		
		public int getFogObjIndex() {
			return fogObjIndex;
		}
		public byte[] getFogObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(fogObjIndex);
		}
		public void setFogObjIndex(byte[] fogObjIndex) {
			this.fogObjIndex = HEXUtils.byteArrayToInt(fogObjIndex);
		}
		public void setFogObjIndexInc(int increaseIds) {
			if (this.fogObjIndex > 0) {
				this.fogObjIndex = this.fogObjIndex + increaseIds;
			}
		}
		
		public int getPolygonModeObjIndex() {
			return polygonModeObjIndex;
		}
		public byte[] getPolygonModeObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(polygonModeObjIndex);
		}
		public void setPolygonModeObjIndex(byte[] polygonModeObjIndex) {
			this.polygonModeObjIndex = HEXUtils.byteArrayToInt(polygonModeObjIndex);
		}
		public void setPolygonModeObjIndexInc(int increaseIds) {
			if (this.polygonModeObjIndex > 0) {
				this.polygonModeObjIndex = this.polygonModeObjIndex + increaseIds;
			}
		}
		
		public int getMaterialObjIndex() {
			return materialObjIndex;
		}
		public byte[] getMaterialObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(materialObjIndex);
		}
		public void setMaterialObjIndex(byte[] materialObjIndex) {
			this.materialObjIndex = HEXUtils.byteArrayToInt(materialObjIndex);
		}
		public void setMaterialObjIndexInc(int increaseIds) {
			if (this.materialObjIndex > 0) {
				this.materialObjIndex = this.materialObjIndex + increaseIds;
			}
		}
		
		public int getTextureRefCount() {
			return textureRefCount;
		}
		public byte[] getTextureRefCountBytes() {
			return HEXUtils.intToByteArrayLE(textureRefCount);
		}
		public void setTextureRefCount(byte[] textureRefCount) {
			this.textureRefCount = HEXUtils.byteArrayToInt(textureRefCount);
		}
		
		public void addToTextureRefObjIndexArray(int objId) {
			this.textureRefObjIndexArray.add(objId);
		}
		public List<Integer> getTextureRefObjIndexArray() {
			return textureRefObjIndexArray;
		}
		public void setTextureRefObjIndexArray(List<Integer> textureRefObjIndexArray) {
			this.textureRefObjIndexArray = textureRefObjIndexArray;
		}
	}
	
	// 0x9
	public static class M3GObjGroup extends M3GObject {
		private byte[] animationControllers;
		private int animationTracks;
		private List<Integer> animationArray = new ArrayList<>(); // ???
		private int parameterCount;
		private List<M3GSubObjParameter> parameterArray = new ArrayList<>();
		private int hasComponentTransform; // byte
		private M3GSubObjComponentTransform componentTransform;
		private int hasGeneralTransform; // byte
		private byte[] generalTransformBytes;
		private byte[] unkPart;
		private int hasUnkFuncPart; // byte
		// here must be something related to unknown functional part
		private int childCount;
		private List<Integer> childObjIndexArray = new ArrayList<>();
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.animationControllers);
			bytes.write(getAnimationTracksBytes());
			for (Integer id : getAnimationArray()) {
				bytes.write(HEXUtils.intToByteArrayLE(id));
			}
			bytes.write(getParameterCountBytes());
			for (M3GSubObjParameter paramObj : getParameterArray()) {
				bytes.write(paramObj.toByteArray());
			}
			bytes.write(getHasComponentTransformByte());
			if (this.hasComponentTransform != 0) {
				bytes.write(this.componentTransform.toByteArray());
			}
			bytes.write(getHasGeneralTransformByte());
			if (this.hasGeneralTransform != 0) {
				bytes.write(this.generalTransformBytes);
			}
			bytes.write(this.unkPart);
			bytes.write(getHasUnkFuncPartByte());
			bytes.write(getChildCountBytes());
			for (Integer id : getChildObjIndexArray()) {
				bytes.write(HEXUtils.intToByteArrayLE(id));
			}
			return bytes.toByteArray();
		}
		
		public byte[] getAnimationControllers() {
			return animationControllers;
		}
		public void setAnimationControllers(byte[] animationControllers) {
			this.animationControllers = animationControllers;
		}
		
		public int getAnimationTracks() {
			return animationTracks;
		}
		public byte[] getAnimationTracksBytes() {
			return HEXUtils.intToByteArrayLE(animationTracks);
		}
		public void setAnimationTracks(byte[] animationTracks) {
			this.animationTracks = HEXUtils.byteArrayToInt(animationTracks);
		}
		
		public void addToAnimationArray(int objId) {
			this.animationArray.add(objId);
		}
		public List<Integer> getAnimationArray() {
			return animationArray;
		}
		public void setAnimationArray(List<Integer> animationArray) {
			this.animationArray = animationArray;
		}
		
		public int getParameterCount() {
			return parameterCount;
		}
		public byte[] getParameterCountBytes() {
			return HEXUtils.intToByteArrayLE(parameterCount);
		}
		public void setParameterCount(byte[] parameterCount) {
			this.parameterCount = HEXUtils.byteArrayToInt(parameterCount);
		}
		
		public void addToParameterArray(M3GSubObjParameter obj) {
			this.parameterArray.add(obj);
		}
		public List<M3GSubObjParameter> getParameterArray() {
			return parameterArray;
		}
		public void setParameterArray(List<M3GSubObjParameter> parameterArray) {
			this.parameterArray = parameterArray;
		}
		
		public int getHasComponentTransform() {
			return hasComponentTransform;
		}
		public byte getHasComponentTransformByte() {
			return (byte)hasComponentTransform;
		}
		public void setHasComponentTransform(byte hasComponentTransform) {
			this.hasComponentTransform = (int)hasComponentTransform;
		}
		
		public M3GSubObjComponentTransform getComponentTransform() {
			return componentTransform;
		}
		public void setComponentTransform(M3GSubObjComponentTransform componentTransform) {
			this.componentTransform = componentTransform;
		}
		
		public int getHasGeneralTransform() {
			return hasGeneralTransform;
		}
		public byte getHasGeneralTransformByte() {
			return (byte)hasGeneralTransform;
		}
		public void setHasGeneralTransform(byte hasGeneralTransform) {
			this.hasGeneralTransform = (int)hasGeneralTransform;
		}
		
		public byte[] getGeneralTransformBytes() {
			return generalTransformBytes;
		}
		public void setGeneralTransformBytes(byte[] generalTransformBytes) {
			this.generalTransformBytes = generalTransformBytes;
		}
		
		public byte[] getUnkPart() {
			return unkPart;
		}
		public void setUnkPart(byte[] unkPart) {
			this.unkPart = unkPart;
		}
		
		public int getHasUnkFuncPart() {
			return hasUnkFuncPart;
		}
		public byte getHasUnkFuncPartByte() {
			return (byte)hasGeneralTransform;
		}
		public void setHasUnkFuncPart(byte hasUnkFuncPart) {
			this.hasUnkFuncPart = (int)hasUnkFuncPart;
		}
		
		public int getChildCount() {
			return childCount;
		}
		public byte[] getChildCountBytes() {
			return HEXUtils.intToByteArrayLE(childCount);
		}
		public void setChildCount(byte[] childCount) {
			this.childCount = HEXUtils.byteArrayToInt(childCount);
		}
		
		public void addToChildObjIndexArray(int objId) {
			this.childObjIndexArray.add(objId);
		}
		public List<Integer> getChildObjIndexArray() {
			return childObjIndexArray;
		}
		public void setChildObjIndexArray(List<Integer> childObjIndexArray) {
			this.childObjIndexArray = childObjIndexArray;
		}
	}
	
	// 0xA
	public static class M3GObjImage2D extends M3GObject {
		private byte[] padding = new byte[8];
		private int parameterCount;
		private List<M3GSubObjParameter> parameterArray = new ArrayList<>();
		private byte unkPostParamByte;
		private int texFormatType; // byte
		private int texWidth;
		private int texHeight;
		private byte[] texMetadataUnkBytes = new byte[4];
		private int texMetadataSize;
		private byte[] texMetadata;
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.padding);
			bytes.write(getParameterCountBytes());
			for (M3GSubObjParameter paramObj : getParameterArray()) {
				bytes.write(paramObj.toByteArray());
			}
			bytes.write(this.unkPostParamByte);
			bytes.write(getTexFormatTypeByte());
			bytes.write(getTexWidthBytes());
			bytes.write(getTexHeightBytes());
			bytes.write(this.texMetadataUnkBytes);
			bytes.write(getTexMetadataSizeBytes());
			bytes.write(this.texMetadata);
			return bytes.toByteArray();
		}
		
		public void addToParameters(M3GSubObjParameter param) {
			this.parameterArray.add(param);
		}
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}
		
		public int getParameterCount() {
			return parameterCount;
		}
		public byte[] getParameterCountBytes() {
			return HEXUtils.intToByteArrayLE(parameterCount);
		}
		public void setParameterCount(byte[] parameterCount) {
			this.parameterCount = HEXUtils.byteArrayToInt(parameterCount);
		}
		
		public void addToParameterArray(M3GSubObjParameter obj) {
			this.parameterArray.add(obj);
		}
		public List<M3GSubObjParameter> getParameterArray() {
			return parameterArray;
		}
		public void setParameterArray(List<M3GSubObjParameter> parameterArray) {
			this.parameterArray = parameterArray;
		}
		
		public byte getUnkPostParamByte() {
			return unkPostParamByte;
		}
		public void setUnkPostParamByte(byte unkPostParamByte) {
			this.unkPostParamByte = unkPostParamByte;
		}
		
		public int getTexFormatType() {
			return texFormatType;
		}
		public byte getTexFormatTypeByte() {
			return (byte)texFormatType;
		}
		public void setTexFormatType(byte texFormatType) {
			this.texFormatType = (int)texFormatType;
		}
		
		public int getTexWidth() {
			return texWidth;
		}
		public byte[] getTexWidthBytes() {
			return HEXUtils.intToByteArrayLE(texWidth);
		}
		public void setTexWidth(byte[] texWidth) {
			this.texWidth = HEXUtils.byteArrayToInt(texWidth);
		}
		
		public int getTexHeight() {
			return texHeight;
		}
		public byte[] getTexHeightBytes() {
			return HEXUtils.intToByteArrayLE(texHeight);
		}
		public void setTexHeight(byte[] texHeight) {
			this.texHeight = HEXUtils.byteArrayToInt(texHeight);
		}
		
		public byte[] getTexMetadataUnkBytes() {
			return texMetadataUnkBytes;
		}
		public void setTexMetadataUnkBytes(byte[] texMetadataUnkBytes) {
			this.texMetadataUnkBytes = texMetadataUnkBytes;
		}

		public int getTexMetadataSize() {
			return texMetadataSize;
		}
		public byte[] getTexMetadataSizeBytes() {
			return HEXUtils.intToByteArrayLE(texMetadataSize);
		}
		public void setTexMetadataSize(byte[] texMetadataSize) {
			this.texMetadataSize = HEXUtils.byteArrayToInt(texMetadataSize);
		}

		public byte[] getTexMetadata() {
			return texMetadata;
		}
		public void setTexMetadata(byte[] texMetadata) {
			this.texMetadata = texMetadata;
		}
	}
	
	// 0xE
	public static class M3GObjMeshConfig extends M3GObject {
		private byte[] padding = new byte[8];
		private byte[] unkPart;
		private int vertexBufferObjIndex;
		private int subMeshCount;
		private List<Integer> subMeshObjIndexArray = new ArrayList<>();
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.padding);
			bytes.write(this.unkPart);
			bytes.write(getVertexBufferObjIndexBytes());
			bytes.write(getSubMeshCountBytes());
			for (Integer id : getSubMeshObjIndexArray()) {
				bytes.write(HEXUtils.intToByteArrayLE(id));
			}
			return bytes.toByteArray();
		}
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}
		
		public byte[] getUnkPart() {
			return unkPart;
		}
		public void setUnkPart(byte[] unkPart) {
			this.unkPart = unkPart;
		}
		
		public int getVertexBufferObjIndex() {
			return vertexBufferObjIndex;
		}
		public byte[] getVertexBufferObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(vertexBufferObjIndex);
		}
		public void setVertexBufferObjIndex(byte[] vertexBufferObjIndex) {
			this.vertexBufferObjIndex = HEXUtils.byteArrayToInt(vertexBufferObjIndex);
		}
		public void setVertexBufferObjIndexInc(int increaseIds) {
			if (this.vertexBufferObjIndex > 0) {
				this.vertexBufferObjIndex = this.vertexBufferObjIndex + increaseIds;
			}
		}
		
		public int getSubMeshCount() {
			return subMeshCount;
		}
		public byte[] getSubMeshCountBytes() {
			return HEXUtils.intToByteArrayLE(subMeshCount);
		}
		public void setSubMeshCount(byte[] subMeshCount) {
			this.subMeshCount = HEXUtils.byteArrayToInt(subMeshCount);
		}
		
		public void addToSubMeshObjIndexArray(int objId) {
			this.subMeshObjIndexArray.add(objId);
		}
		public List<Integer> getSubMeshObjIndexArray() {
			return subMeshObjIndexArray;
		}
		public void setSubMeshObjIndexArray(List<Integer> subMeshObjIndexArray) {
			this.subMeshObjIndexArray = subMeshObjIndexArray;
		}
	}
	
	// 0x11
	public static class M3GObjTextureRef extends M3GObject {
		private byte[] padding = new byte[12];
		private byte[] unkPart = new byte[2];
		private int image2DObjIndex;
		private byte[] unkPart2 = new byte[8];
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.padding);
			bytes.write(this.unkPart);
			bytes.write(getImage2DObjIndexBytes());
			bytes.write(this.unkPart2);
			return bytes.toByteArray();
		}
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}
		
		public byte[] getUnkPart() {
			return unkPart;
		}
		public void setUnkPart(byte[] unkPart) {
			this.unkPart = unkPart;
		}
		
		public int getImage2DObjIndex() {
			return image2DObjIndex;
		}
		public byte[] getImage2DObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(image2DObjIndex);
		}
		public void setImage2DObjIndex(byte[] image2DObjIndex) {
			this.image2DObjIndex = HEXUtils.byteArrayToInt(image2DObjIndex);
		}
		public void setImage2DObjIndexInc(int increaseIds) {
			if (this.image2DObjIndex > 0) {
				this.image2DObjIndex = this.image2DObjIndex + increaseIds;
			}
		}
		
		public byte[] getUnkPart2() {
			return unkPart2;
		}
		public void setUnkPart2(byte[] unkPart2) {
			this.unkPart2 = unkPart2;
		}
	}
	
	// 0x14
	public static class M3GObjVertexArray extends M3GObject {
		private byte[] padding = new byte[12];
		private int componentSize; // byte
		private int componentCount; // byte
		private int encoding; // byte
		private int vertexCount; // short
		private List<float[]> vertexArray = new ArrayList<>();
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.padding);
			bytes.write(getComponentSizeByte());
			bytes.write(getComponentCountByte());
			bytes.write(getEncodingByte());
			bytes.write(getVertexCountBytes());
			for (float[] vertexObj : getVertexArray()) {
				for (float value : vertexObj) {
					byte[] floatBytes = HEXUtils.floatToBytes(value);
					bytes.write(floatBytes);
				}
			}
			return bytes.toByteArray();
		}
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}
		
		public int getComponentSize() {
			return componentSize;
		}
		public byte getComponentSizeByte() {
			return (byte)componentSize;
		}
		public void setComponentSize(byte componentSize) {
			this.componentSize = (int)componentSize;
		}
		
		public int getComponentCount() {
			return componentCount;
		}
		public byte getComponentCountByte() {
			return (byte)componentCount;
		}
		public void setComponentCount(byte componentCount) {
			this.componentCount = (int)componentCount;
		}
		
		public int getEncoding() {
			return encoding;
		}
		public byte getEncodingByte() {
			return (byte)encoding;
		}
		public void setEncoding(byte encoding) {
			this.encoding = (int)encoding;
		}
		
		public int getVertexCount() {
			return vertexCount;
		}
		public byte[] getVertexCountBytes() {
			return HEXUtils.shortToBytes(vertexCount);
		}
		public void setVertexCount(byte[] vertexCount) {
			this.vertexCount = HEXUtils.twoLEByteArrayToInt(vertexCount);
		}
		
		public void addToVertexArray(float[] obj) {
			this.vertexArray.add(obj);
		}
		public List<float[]> getVertexArray() {
			return vertexArray;
		}
		public void setVertexArray(List<float[]> vertexArray) {
			this.vertexArray = vertexArray;
		}
	}
	
	// 0x15
	public static class M3GObjVertexBuffer extends M3GObject {
		private byte[] padding = new byte[12];
		private byte[] colorRGBA;
		private int positionVertexArrayObjIndex;
		private float[] positionBias = new float[3];
		private float positionScale;
		private int normalsVertexArrayObjIndex;
		private int colorsObjIndex;
		private int textureCoordArrayCount;
		private List<M3GSubObjTextureCoord> textureCoordArray = new ArrayList<>();
		private int tangentsVertexArrayObjIndex;
		private int binormalsVertexArrayObjIndex;
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.padding);
			bytes.write(this.colorRGBA);
			bytes.write(getPositionVertexArrayObjIndexBytes());
			bytes.write(HEXUtils.floatToBytes(this.positionBias[0]));
			bytes.write(HEXUtils.floatToBytes(this.positionBias[1]));
			bytes.write(HEXUtils.floatToBytes(this.positionBias[2]));
			bytes.write(getPositionScaleBytes());
			bytes.write(getNormalsVertexArrayObjIndexBytes());
			bytes.write(getColorsObjIndexBytes());
			bytes.write(getTextureCoordArrayCountBytes());
			for (M3GSubObjTextureCoord texCoordObj : this.textureCoordArray) {
				bytes.write(texCoordObj.toByteArray());
			}
			bytes.write(getTangentsVertexArrayObjIndexBytes());
			bytes.write(getBinormalsVertexArrayObjIndexBytes());
			return bytes.toByteArray();
		}
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}
		
		public byte[] getColorRGBA() {
			return colorRGBA;
		}
		public void setColorRGBA(byte[] colorRGBA) {
			this.colorRGBA = colorRGBA;
		}
		
		public int getPositionVertexArrayObjIndex() {
			return positionVertexArrayObjIndex;
		}
		public byte[] getPositionVertexArrayObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(positionVertexArrayObjIndex);
		}
		public void setPositionVertexArrayObjIndex(byte[] positionVertexArrayObjIndex) {
			this.positionVertexArrayObjIndex = HEXUtils.byteArrayToInt(positionVertexArrayObjIndex);
		}
		public void setPositionVertexArrayObjIndexInc(int increaseIds) {
			if (this.positionVertexArrayObjIndex > 0) {
				this.positionVertexArrayObjIndex = this.positionVertexArrayObjIndex + increaseIds;
			}
		}
		
		public float[] getPositionBias() {
			return positionBias;
		}
		public void setPositionBias(float[] positionBias) {
			this.positionBias = positionBias;
		}
		
		public float getPositionScale() {
			return positionScale;
		}
		public byte[] getPositionScaleBytes() {
			return HEXUtils.floatToBytes(positionScale);
		}
		public void setPositionScale(byte[] positionScale) {
			this.positionScale = HEXUtils.bytesToFloat(positionScale);
		}
		
		public int getNormalsVertexArrayObjIndex() {
			return normalsVertexArrayObjIndex;
		}
		public byte[] getNormalsVertexArrayObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(normalsVertexArrayObjIndex);
		}
		public void setNormalsVertexArrayObjIndex(byte[] normalsVertexArrayObjIndex) {
			this.normalsVertexArrayObjIndex = HEXUtils.byteArrayToInt(normalsVertexArrayObjIndex);
		}
		public void setNormalsVertexArrayObjIndexInc(int increaseIds) {
			if (this.normalsVertexArrayObjIndex > 0) {
				this.normalsVertexArrayObjIndex = this.normalsVertexArrayObjIndex + increaseIds;
			}
		}
		
		public int getColorsObjIndex() {
			return colorsObjIndex;
		}
		public byte[] getColorsObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(colorsObjIndex);
		}
		public void setColorsObjIndex(byte[] colorsObjIndex) {
			this.colorsObjIndex = HEXUtils.byteArrayToInt(colorsObjIndex);
		}
		public void setColorsObjIndexInc(int increaseIds) {
			if (this.colorsObjIndex > 0) {
				this.colorsObjIndex = this.colorsObjIndex + increaseIds;
			}
		}
		
		public int getTextureCoordArrayCount() {
			return textureCoordArrayCount;
		}
		public byte[] getTextureCoordArrayCountBytes() {
			return HEXUtils.intToByteArrayLE(textureCoordArrayCount);
		}
		public void setTextureCoordArrayCount(byte[] textureCoordArrayCount) {
			this.textureCoordArrayCount = HEXUtils.byteArrayToInt(textureCoordArrayCount);
		}
		
		public void addToTextureCoordArray(M3GSubObjTextureCoord obj) {
			this.textureCoordArray.add(obj);
		}
		public List<M3GSubObjTextureCoord> getTextureCoordArray() {
			return textureCoordArray;
		}
		public void setTextureCoordArray(List<M3GSubObjTextureCoord> textureCoordArray) {
			this.textureCoordArray = textureCoordArray;
		}
		
		public int getTangentsVertexArrayObjIndex() {
			return tangentsVertexArrayObjIndex;
		}
		public byte[] getTangentsVertexArrayObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(tangentsVertexArrayObjIndex);
		}
		public void setTangentsVertexArrayObjIndex(byte[] tangentsVertexArrayObjIndex) {
			this.tangentsVertexArrayObjIndex = HEXUtils.byteArrayToInt(tangentsVertexArrayObjIndex);
		}
		public void setTangentsVertexArrayObjIndexInc(int increaseIds) {
			if (this.tangentsVertexArrayObjIndex > 0) {
				this.tangentsVertexArrayObjIndex = this.tangentsVertexArrayObjIndex + increaseIds;
			}
		}
		
		public int getBinormalsVertexArrayObjIndex() {
			return binormalsVertexArrayObjIndex;
		}
		public byte[] getBinormalsVertexArrayObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(binormalsVertexArrayObjIndex);
		}
		public void setBinormalsVertexArrayObjIndex(byte[] binormalsVertexArrayObjIndex) {
			this.binormalsVertexArrayObjIndex = HEXUtils.byteArrayToInt(binormalsVertexArrayObjIndex);
		}
		public void setBinormalsVertexArrayObjIndexInc(int increaseIds) {
			if (this.binormalsVertexArrayObjIndex > 0) {
				this.binormalsVertexArrayObjIndex = this.binormalsVertexArrayObjIndex + increaseIds;
			}
		}
	}
	
	// 0x64
	public static class M3GObjSubMesh extends M3GObject {
		private byte[] padding = new byte[8];
		private int parameterCount;
		private List<M3GSubObjParameter> parameterArray = new ArrayList<>();
		private int indexBufferObjIndex;
		private int appearanceObjIndex;
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.padding);
			bytes.write(getParameterCountBytes());
			for (M3GSubObjParameter paramObj : getParameterArray()) {
				bytes.write(paramObj.toByteArray());
			}
			bytes.write(getIndexBufferObjIndexBytes());
			bytes.write(getAppearanceObjIndexBytes());
			return bytes.toByteArray();
		}
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}
		
		public int getParameterCount() {
			return parameterCount;
		}
		public byte[] getParameterCountBytes() {
			return HEXUtils.intToByteArrayLE(parameterCount);
		}
		public void setParameterCount(byte[] parameterCount) {
			this.parameterCount = HEXUtils.byteArrayToInt(parameterCount);
		}
		
		public void addToParameterArray(M3GSubObjParameter obj) {
			this.parameterArray.add(obj);
		}
		public List<M3GSubObjParameter> getParameterArray() {
			return parameterArray;
		}
		public void setParameterArray(List<M3GSubObjParameter> parameterArray) {
			this.parameterArray = parameterArray;
		}
		
		public int getIndexBufferObjIndex() {
			return indexBufferObjIndex;
		}
		public byte[] getIndexBufferObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(indexBufferObjIndex);
		}
		public void setIndexBufferObjIndex(byte[] indexBufferObjIndex) {
			this.indexBufferObjIndex = HEXUtils.byteArrayToInt(indexBufferObjIndex);
		}
		public void setIndexBufferObjIndexInc(int increaseIds) {
			if (this.indexBufferObjIndex > 0) {
				this.indexBufferObjIndex = this.indexBufferObjIndex + increaseIds;
			}
		}
		
		public int getAppearanceObjIndex() {
			return appearanceObjIndex;
		}
		public byte[] getAppearanceObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(appearanceObjIndex);
		}
		public void setAppearanceObjIndex(byte[] appearanceObjIndex) {
			this.appearanceObjIndex = HEXUtils.byteArrayToInt(appearanceObjIndex);
		}
		public void setAppearanceObjIndexInc(int increaseIds) {
			if (this.appearanceObjIndex > 0) {
				this.appearanceObjIndex = this.appearanceObjIndex + increaseIds;
			}
		}
	}
	
	// 0x65
	public static class M3GObjIndexBuffer extends M3GObject {
		private byte[] padding = new byte[12];
		private int encoding; // byte
		private int indexCount;
		private List<Integer> indexArray = new ArrayList<>();
		
		@Override
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(getTypeByte());
			bytes.write(getSizeBytes());
			//
			bytes.write(this.padding);
			bytes.write(getEncodingByte());
			bytes.write(getIndexCountBytes());
			for (Integer id : getIndexArray()) {
				bytes.write(HEXUtils.intToByteArrayLE(id));
			}
			return bytes.toByteArray();
		}
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}
		
		public int getEncoding() {
			return encoding;
		}
		public byte getEncodingByte() {
			return (byte)encoding;
		}
		public void setEncoding(byte encoding) {
			this.encoding = (int)encoding;
		}
		
		public int getIndexCount() {
			return indexCount;
		}
		public byte[] getIndexCountBytes() {
			return HEXUtils.intToByteArrayLE(indexCount);
		}
		public void setIndexCount(byte[] indexCount) {
			this.indexCount = HEXUtils.byteArrayToInt(indexCount);
		}
		
		public void addToIndexArray(int objId) {
			this.indexArray.add(objId);
		}
		public List<Integer> getIndexArray() {
			return indexArray;
		}
		public void setIndexArray(List<Integer> indexArray) {
			this.indexArray = indexArray;
		}
	}
}
