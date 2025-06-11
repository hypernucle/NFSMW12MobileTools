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
		private int type; // byte
		private int size;
		
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

		public byte[] getData() {
			return data;
		}
		public void setData(byte[] data) {
			this.data = data;
		}
	}
	
	public static class M3GSubObjParameter {
		private int type;
		private int size;
		private byte[] value;
		
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
		
		public byte[] getValue() {
			return value;
		}
		public void setValue(byte[] value) {
			this.value = value;
		}
	}
	
	public static class M3GSubObjComponentTransform {
		private float[] translation = new float[3];
		private float[] scale = new float[3];
		private float orientationAngle;
		private float orientationAxisX;
		private float orientationAxisY;
		private float orientationAxisZ;
		
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
		private float[] textureCoordBias;
		private float textureCoordScale;
		
		public int getTextureCoordObjIndex() {
			return textureCoordObjIndex;
		}
		public byte[] getTextureCoordObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(textureCoordObjIndex);
		}
		public void setTextureCoordObjIndex(byte[] textureCoordObjIndex) {
			this.textureCoordObjIndex = HEXUtils.byteArrayToInt(textureCoordObjIndex);
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
	
	// 0x3
	public static class M3GObjAppearance extends M3GObject {
		private byte[] animationControllers;
		private int animationTracks;
		private List<M3GObjGeneric> animationArray = new ArrayList<>(); // ???
		private int parameterCount;
		private List<M3GSubObjParameter> parameterArray = new ArrayList<>();
		private int layer; // byte
		private int compositingModeObjIndex;
		private int fogObjIndex;
		private int polygonModeObjIndex;
		private int materialObjIndex;
		private int textureRefCount;
		private List<Integer> textureRefObjIndexArray = new ArrayList<>();
		
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
		
		public List<M3GObjGeneric> getAnimationArray() {
			return animationArray;
		}
		public void setAnimationArray(List<M3GObjGeneric> animationArray) {
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
		
		public int getFogObjIndex() {
			return fogObjIndex;
		}
		public byte[] getFogObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(fogObjIndex);
		}
		public void setFogObjIndex(byte[] fogObjIndex) {
			this.fogObjIndex = HEXUtils.byteArrayToInt(fogObjIndex);
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
		
		public int getMaterialObjIndex() {
			return materialObjIndex;
		}
		public byte[] getMaterialObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(materialObjIndex);
		}
		public void setMaterialObjIndex(byte[] materialObjIndex) {
			this.materialObjIndex = HEXUtils.byteArrayToInt(materialObjIndex);
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
		private List<M3GObjGeneric> animationArray = new ArrayList<>(); // ???
		private int parameterCount;
		private List<M3GSubObjParameter> parameterArray = new ArrayList<>();
		private int hasComponentTransform; // byte
		private List<M3GSubObjComponentTransform> componentTransform = new ArrayList<>();
		private int hasGeneralTransform; // byte
		private byte[] unkPart;
		private int hasUnkFuncPart; // byte
		// here must be something related to unknown functional part
		private int childCount;
		private int childObjIndex;
		
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
		
		public List<M3GObjGeneric> getAnimationArray() {
			return animationArray;
		}
		public void setAnimationArray(List<M3GObjGeneric> animationArray) {
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
		
		public List<M3GSubObjComponentTransform> getComponentTransform() {
			return componentTransform;
		}
		public void setComponentTransform(List<M3GSubObjComponentTransform> componentTransform) {
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
		
		public int getChildObjIndex() {
			return childObjIndex;
		}
		public byte[] getChildObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(childObjIndex);
		}
		public void setChildObjIndex(byte[] childObjIndex) {
			this.childObjIndex = HEXUtils.byteArrayToInt(childObjIndex);
		}
	}
	
	// 0xA
	public static class M3GObjImage2D extends M3GObject {
		private byte[] padding = new byte[8];
		private int parameterCount;
		private List<M3GSubObjParameter> parameterArray = new ArrayList<>();
		private byte unkPostParamByte = 0x63;
		private byte texFormatType;
		private int texWidth;
		private int texHeight;
		private byte[] texMetadata;
		
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
		
		public byte getTexFormatType() {
			return texFormatType;
		}
		public void setTexFormatType(byte texFormatType) {
			this.texFormatType = texFormatType;
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
		
		public int getSubMeshCount() {
			return subMeshCount;
		}
		public byte[] getSubMeshCountBytes() {
			return HEXUtils.intToByteArrayLE(vertexBufferObjIndex);
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
		private byte[] unkPart;
		private int image2DObjIndex;
		private byte[] unkPart2;
		
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
		private byte[] vertexArray;
		
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
		
		public byte[] getVertexArray() {
			return vertexArray;
		}
		public void setVertexArray(byte[] vertexArray) {
			this.vertexArray = vertexArray;
		}
	}
	
	// 0x15
	public static class M3GObjVertexBuffer extends M3GObject {
		private byte[] padding = new byte[12];
		private float colorRGBA;
		private int positionVertexArrayObjIndex;
		private float[] positionBias = new float[3];
		private float positionScale;
		private int normalsVertexArrayObjIndex;
		private int colorsObjIndex;
		private int textureCoordArrayCount;
		private List<M3GSubObjTextureCoord> textureCoordArray = new ArrayList<>();
		private byte[] tangents;
		private byte[] binormals;
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}
		
		public float getColorRGBA() {
			return colorRGBA;
		}
		public byte[] getColorRGBABytes() {
			return HEXUtils.floatToBytes(colorRGBA);
		}
		public void setColorRGBA(byte[] colorRGBA) {
			this.colorRGBA = HEXUtils.bytesToFloat(colorRGBA);
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
		
		public int getColorsObjIndex() {
			return colorsObjIndex;
		}
		public byte[] getColorsObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(colorsObjIndex);
		}
		public void setColorsObjIndex(byte[] colorsObjIndex) {
			this.colorsObjIndex = HEXUtils.byteArrayToInt(colorsObjIndex);
		}
		
		public int getTextureCoordArrayCount() {
			return textureCoordArrayCount;
		}
		public byte[] getTextureCoordArrayCounBytes() {
			return HEXUtils.intToByteArrayLE(textureCoordArrayCount);
		}
		public void setTextureCoordArrayCoun(byte[] textureCoordArrayCount) {
			this.textureCoordArrayCount = HEXUtils.byteArrayToInt(textureCoordArrayCount);
		}
		
		public List<M3GSubObjTextureCoord> getTextureCoordArray() {
			return textureCoordArray;
		}
		public void setTextureCoordArray(List<M3GSubObjTextureCoord> textureCoordArray) {
			this.textureCoordArray = textureCoordArray;
		}
		
		public byte[] getTangents() {
			return tangents;
		}
		public void setTangents(byte[] tangents) {
			this.tangents = tangents;
		}
		
		public byte[] getBinormals() {
			return binormals;
		}
		public void setBinormals(byte[] binormals) {
			this.binormals = binormals;
		}
	}
	
	// 0x64
	public static class M3GObjSubMesh extends M3GObject {
		private byte[] padding = new byte[12];
		private int indexBufferObjIndex;
		private int appearanceObjIndex;
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
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
		
		public int getAppearanceObjIndex() {
			return appearanceObjIndex;
		}
		public byte[] getAppearanceObjIndexBytes() {
			return HEXUtils.intToByteArrayLE(appearanceObjIndex);
		}
		public void setAppearanceObjIndex(byte[] appearanceObjIndex) {
			this.appearanceObjIndex = HEXUtils.byteArrayToInt(appearanceObjIndex);
		}
	}
	
	// 0x65
	public static class M3GObjIndexBuffer extends M3GObject {
		private byte[] padding = new byte[12];
		private int encoding; // byte
		private int indexCount;
		private List<Integer> indexArray = new ArrayList<>();
		
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
