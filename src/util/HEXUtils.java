package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import util.DataClasses.SBinCDATEntry;

public final class HEXUtils {
	private HEXUtils() {}
	
	public static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);

	public static void addElementsToByteArraysList(List<String> list, List<byte[]> blockElements) {
		for (String entry : list) {
			blockElements.add(entry.getBytes(StandardCharsets.UTF_8));
		}
	}

	public static void addCDATElementsToByteArraysList(List<SBinCDATEntry> list, List<byte[]> blockElements) {
		for (SBinCDATEntry entry : list) {
			blockElements.add(entry.getString().getBytes(StandardCharsets.UTF_8));
		}
	}

	public static void writeBytesWithAddition(ByteArrayOutputStream hexStream, int baseValue, int addition) throws IOException {
		hexStream.write(intToByteArrayLE(baseValue + addition));
	}

	public static int byteArrayToInt(byte[] bytes) {
		int beInt = ((bytes[0] & 0xFF) << 24) | 
				((bytes[1] & 0xFF) << 16) | 
				((bytes[2] & 0xFF) << 8) | 
				((bytes[3] & 0xFF) << 0);
		return hexRev(beInt);
	}

	public static int twoLEByteArrayToInt(byte[] bytes) {
		return ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF);
	}

	public static byte[] intToByteArrayLE(int data) {    
		return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data).array(); 
	}
	public static byte[] intToByteArrayBE(int data) {    
		return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(data).array(); 
	}
	
	public static String byteToHexString(byte value) {
		return Integer.toString(value, 16);
	}

	public static short bytesToShort(byte[] bytes) {
		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
	}
	public static byte[] shortToBytes(int data) {
		return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short)data).array();
	}
	public static byte[] shortToBytesBE(int data) {
		return ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort((short)data).array();
	}
	
	public static float bytesToFloat(byte[] bytes) {
		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
	}
	public static byte[] floatToBytes(float value) {  
	    return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array();
	}
	
	public static double bytesToDouble(byte[] bytes) {
	    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble();
	}
	public static byte[] doubleToBytes(double value) {  
	     return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array();
	}
	
	public static String setDataEntryHexId(int i, boolean longDataElementIds) {
		return HEXUtils.hexToString(longDataElementIds ? HEXUtils.intToByteArrayLE(i) : HEXUtils.shortToBytes(i));
	}
	
	public static byte[] setDataEntryHexIdBytes(int i, boolean longDataElementIds) {
		return longDataElementIds ? HEXUtils.intToByteArrayLE(i) : HEXUtils.shortToBytes(i);
	}

	// Taken from StackOverflow (maybeWeCouldStealAVan)
	public static String hexToString(byte[] bytes) {
		byte[] hexChars = new byte[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars, StandardCharsets.UTF_8).toUpperCase();
	}

	public static byte[] decodeHexStr(String str) {
		int len = str.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4)
					+ Character.digit(str.charAt(i+1), 16));
		}
		return data;
	}
	
	public static byte[] stringToBytes(String str) {
		return str.getBytes(StandardCharsets.UTF_8);
	}
	
	public static String utf8BytesToString(byte[] value) {
		return new String(value, StandardCharsets.UTF_8);
	}
	
	public static int strHexToInt(String hexStr) {
		byte[] bytes = decodeHexStr(hexStr);
		return bytes.length != 4 ? twoLEByteArrayToInt(bytes) : byteArrayToInt(bytes);
	}

	// https://stackoverflow.com/a/66638297
	public static List<byte[]> splitByteArray(byte[] array, int chunkSize) {
		List<byte[]> chunks = new ArrayList<>();
		for (int i = 0; i < array.length; ) {
			byte[] chunk = new byte[Math.min(chunkSize, array.length - i)];
			for (int j = 0; j < chunk.length; j++, i++) {
				chunk[j] = array[i];
			}
			chunks.add(chunk);
		}
		return chunks;
	}

	public static int hexRev(int n)
	{
		return ((n >> 24) & 0xff)
				| // (n >> 24) - 0x000000aa
				((n << 8) & 0xff0000)
				| // (n << 24) - 0xdd000000
				((n >> 8) & 0xff00)
				| // (((n >> 16) << 24) >> 16) - 0xbb00
				((n << 24) & 0xff000000); // (((n >> 8) << 24)
		// >> 8) - 0xcc0000
		// If output of all the above expression is
		// OR'ed then it results in 0xddccbbaa
	}
	
	public static Checksum getFileBytesChecksum(byte[] data) {
		Checksum crc = new CRC32();
        crc.update(data, 0, data.length);
        return crc;
	}
}
