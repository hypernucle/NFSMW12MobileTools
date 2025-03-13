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
	
	public static class SBinPlaylistEntryHex {
		private int ohdrDescRemainder = 0;
		private int ohdrStruRemainder = 0;
		private byte[] header = SHORT_EMPTYBYTES;
		private byte[] unkHex1 = new byte[10];
		private byte[] name = SHORT_EMPTYBYTES;
		private byte[] unkHex2 = new byte[10];
		private byte[] orderId = SHORT_EMPTYBYTES;
		private byte[] padding = SHORT_EMPTYBYTES;
		private SBinStructureEntryHex tracksMap;
		private List<SBinPlaylistTrackHex> tracks = new ArrayList<>();
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(this.header);
			bytes.write(this.unkHex1);
			bytes.write(this.name);
			bytes.write(this.unkHex2);
			bytes.write(this.orderId);
			bytes.write(this.padding);
			bytes.write(this.tracksMap.toByteArray());
			for (SBinPlaylistTrackHex obj : this.tracks) {
				bytes.write(obj.toByteArray());
			}
			return bytes.toByteArray();
		}
		
		public List<SBinOHDREntry> ohdrMapTemplate() {
			List<SBinOHDREntry> ohdrEntries = new ArrayList<>();
			ohdrEntries.add(new SBinOHDREntry(0x1C, this.ohdrDescRemainder));
			ohdrEntries.add(new SBinOHDREntry(this.tracksMap.getByteSize(), this.ohdrStruRemainder));
			for (SBinPlaylistTrackHex obj : this.tracks) {
				ohdrEntries.add(new SBinOHDREntry(obj.getByteSize(), obj.getOhdrUnkRemainder()));
			}
			return ohdrEntries;
		}
		
		public void addToPlaylistTracks(SBinPlaylistTrackHex trackObj) {
			this.tracks.add(trackObj);
		}

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

		public byte[] getHeader() {
			return header;
		}
		public void setHeader(byte[] header) {
			this.header = header;
		}

		public byte[] getUnkHex1() {
			return unkHex1;
		}
		public void setUnkHex1(byte[] unkHex1) {
			this.unkHex1 = unkHex1;
		}

		public byte[] getName() {
			return name;
		}
		public void setName(byte[] name) {
			this.name = name;
		}

		public byte[] getUnkHex2() {
			return unkHex2;
		}
		public void setUnkHex2(byte[] unkHex2) {
			this.unkHex2 = unkHex2;
		}

		public byte[] getOrderId() {
			return orderId;
		}
		public void setOrderId(byte[] orderId) {
			this.orderId = orderId;
		}

		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
		}

		public SBinStructureEntryHex getTracksMap() {
			return tracksMap;
		}
		public void setTracksMap(SBinStructureEntryHex tracksMap) {
			this.tracksMap = tracksMap;
		}

		public List<SBinPlaylistTrackHex> getTracks() {
			return tracks;
		}
		public void setTracks(List<SBinPlaylistTrackHex> tracks) {
			this.tracks = tracks;
		}
	}
	
	public static class SBinPlaylistTrackHex {
		private int ohdrUnkRemainder = 0;
		private byte[] header = SHORT_EMPTYBYTES;
		private byte[] unkHex1 = new byte[10];
		private byte[] filePath = SHORT_EMPTYBYTES;
		private byte[] unkHex2 = new byte[8];
		private byte[] artist = SHORT_EMPTYBYTES;
		private byte[] unkHex3 = new byte[8];
		private byte[] title = SHORT_EMPTYBYTES;
		private byte[] padding = SHORT_EMPTYBYTES;
		
		private int byteSize = 0x24;
		
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bytes.write(this.header);
			bytes.write(this.unkHex1);
			bytes.write(this.filePath);
			bytes.write(this.unkHex2);
			bytes.write(this.artist);
			bytes.write(this.unkHex3);
			bytes.write(this.title);
			bytes.write(this.padding);
			return bytes.toByteArray();
		}
		
		public Integer getByteSize() {
			return this.byteSize;
		}
		
		public int getOhdrUnkRemainder() {
			return ohdrUnkRemainder;
		}
		public void setOhdrUnkRemainder(int ohdrUnkRemainder) {
			this.ohdrUnkRemainder = ohdrUnkRemainder;
		}
		
		public byte[] getHeader() {
			return header;
		}
		public void setHeader(byte[] header) {
			this.header = header;
		}

		public byte[] getUnkHex1() {
			return unkHex1;
		}
		public void setUnkHex1(byte[] unkHex1) {
			this.unkHex1 = unkHex1;
		}
		
		public byte[] getFilePath() {
			return filePath;
		}
		public void setFilePath(byte[] filePath) {
			this.filePath = filePath;
		}
		
		public byte[] getUnkHex2() {
			return unkHex2;
		}
		public void setUnkHex2(byte[] unkHex2) {
			this.unkHex2 = unkHex2;
		}
		
		public byte[] getArtist() {
			return artist;
		}
		public void setArtist(byte[] artist) {
			this.artist = artist;
		}
		
		public byte[] getUnkHex3() {
			return unkHex3;
		}
		public void setUnkHex3(byte[] unkHex3) {
			this.unkHex3 = unkHex3;
		}
		
		public byte[] getTitle() {
			return title;
		}
		public void setTitle(byte[] title) {
			this.title = title;
		}
		
		public byte[] getPadding() {
			return padding;
		}
		public void setPadding(byte[] padding) {
			this.padding = padding;
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

}
