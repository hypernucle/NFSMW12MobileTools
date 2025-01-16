import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import util.FNV1;
import util.DataClasses.SBinBlockObj;
import util.DataClasses.SBinJson;

public class SBin {
	
	Gson gson = new Gson();
	
	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
	private static final byte[] SBIN_HEADER = "SBIN".getBytes(StandardCharsets.UTF_8);
	private static final byte[] ENUM_HEADER = "ENUM".getBytes(StandardCharsets.UTF_8);
	private static final byte[] STRU_HEADER = "STRU".getBytes(StandardCharsets.UTF_8);
	private static final byte[] FIEL_HEADER = "FIEL".getBytes(StandardCharsets.UTF_8);
	private static final byte[] OHDR_HEADER = "OHDR".getBytes(StandardCharsets.UTF_8);
	private static final byte[] DATA_HEADER = "DATA".getBytes(StandardCharsets.UTF_8);
	private static final byte[] CHDR_HEADER = "CHDR".getBytes(StandardCharsets.UTF_8);
	private static final byte[] CDAT_HEADER = "CDAT".getBytes(StandardCharsets.UTF_8);
	
	private static int curPos = 0x0;
	
	//
	
	public void unpackSBin(String filePath) throws IOException {
		SBinJson sbinJson = new SBinJson();
		Path sbinFilePath = Paths.get(filePath);
		byte[] sbinData = Files.readAllBytes(sbinFilePath);
		
		int sbinVersion = sbinData[4];
		if (sbinVersion != 0x03) {
			System.out.println("!!! This SBin version is not supported, version 3 required.");
			return;
		}
		changeCurPos(0x8); // Skip SBin header + version
		
		// ENUM
		SBinBlockObj enumBlock = processSBinBlock(sbinData, ENUM_HEADER, STRU_HEADER);		
		sbinJson.setENUMHexStr(hexToString(enumBlock.getBlockBytes()).toUpperCase());
		sbinJson.setENUMHexEmptyBytesCount(enumBlock.getBlockEmptyBytesCount());
		// STRU
		SBinBlockObj struBlock = processSBinBlock(sbinData, STRU_HEADER, FIEL_HEADER);
		sbinJson.setSTRUHexStr(hexToString(struBlock.getBlockBytes()).toUpperCase());
		sbinJson.setSTRUHexEmptyBytesCount(struBlock.getBlockEmptyBytesCount());
		// FIEL
		SBinBlockObj fielBlock = processSBinBlock(sbinData, FIEL_HEADER, OHDR_HEADER);	
		sbinJson.setFIELHexStr(hexToString(fielBlock.getBlockBytes()).toUpperCase());
		sbinJson.setFIELHexEmptyBytesCount(fielBlock.getBlockEmptyBytesCount());
		// OHDR
		SBinBlockObj ohdrBlock = processSBinBlock(sbinData, OHDR_HEADER, DATA_HEADER);		
		sbinJson.setOHDRHexStr(hexToString(ohdrBlock.getBlockBytes()).toUpperCase());
		sbinJson.setOHDRHexEmptyBytesCount(ohdrBlock.getBlockEmptyBytesCount());
		// DATA
		SBinBlockObj dataBlock = processSBinBlock(sbinData, DATA_HEADER, CHDR_HEADER);	
		sbinJson.setDATAHexStr(hexToString(dataBlock.getBlockBytes()).toUpperCase());
		sbinJson.setDATAHexEmptyBytesCount(dataBlock.getBlockEmptyBytesCount());
		// CHDR
		SBinBlockObj chdrBlock = processSBinBlock(sbinData, CHDR_HEADER, CDAT_HEADER);
		sbinJson.setCHDRHexStr(hexToString(chdrBlock.getBlockBytes()).toUpperCase());
		sbinJson.setCHDRHexEmptyBytesCount(chdrBlock.getBlockEmptyBytesCount());
		// CDAT
		SBinBlockObj cdatBlock = processSBinBlock(sbinData, CDAT_HEADER, null);
		sbinJson.setCDATHexStr(hexToString(cdatBlock.getBlockBytes()).toUpperCase());
		sbinJson.setCDATHexEmptyBytesCount(cdatBlock.getBlockEmptyBytesCount());
		
		sbinJson.setFileName(sbinFilePath.getFileName().toString());
		sbinJson.setSbinVersion(sbinVersion);
		
		gson = new GsonBuilder().setPrettyPrinting().create();
	    String jsonOut = gson.toJson(sbinJson);
	    Files.write(Paths.get("test.json"), jsonOut.getBytes(StandardCharsets.UTF_8));
	}
	
	public void repackSBin(String filePath) throws IOException {
		Reader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
		SBinJson sbinJsonObj = new Gson().fromJson(reader, new TypeToken<SBinJson>(){}.getType());
	    reader.close();
	    
	    // ENUM
	    SBinBlockObj enumBlock = createSBinBlock(sbinJsonObj.getENUMHexStr(), ENUM_HEADER);
	    enumBlock.setBlockEmptyBytesCount(sbinJsonObj.getENUMHexEmptyBytesCount());
	    byte[] finalENUM = buildSBinBlock(enumBlock);
	    // STRU
	    SBinBlockObj struBlock = createSBinBlock(sbinJsonObj.getSTRUHexStr(), STRU_HEADER);
	    struBlock.setBlockEmptyBytesCount(sbinJsonObj.getSTRUHexEmptyBytesCount());
	    byte[] finalSTRU = buildSBinBlock(struBlock);
	    // FIEL
	    SBinBlockObj fielBlock = createSBinBlock(sbinJsonObj.getFIELHexStr(), FIEL_HEADER);
	    fielBlock.setBlockEmptyBytesCount(sbinJsonObj.getFIELHexEmptyBytesCount());
	    byte[] finalFIEL = buildSBinBlock(fielBlock);
	    // OHDR
	    SBinBlockObj ohdrBlock = createSBinBlock(sbinJsonObj.getOHDRHexStr(), OHDR_HEADER);
	    ohdrBlock.setBlockEmptyBytesCount(sbinJsonObj.getOHDRHexEmptyBytesCount());
	    byte[] finalOHDR = buildSBinBlock(ohdrBlock);
	    // DATA
	    SBinBlockObj dataBlock = createSBinBlock(sbinJsonObj.getDATAHexStr(), DATA_HEADER);
	    dataBlock.setBlockEmptyBytesCount(sbinJsonObj.getDATAHexEmptyBytesCount());
	    byte[] finalDATA = buildSBinBlock(dataBlock);
	    // CHDR
	    SBinBlockObj chdrBlock = createSBinBlock(sbinJsonObj.getCHDRHexStr(), CHDR_HEADER);
	    chdrBlock.setBlockEmptyBytesCount(sbinJsonObj.getCHDRHexEmptyBytesCount());
	    byte[] finalCHDR = buildSBinBlock(chdrBlock);
	    // CDAT
	    SBinBlockObj cdatBlock = createSBinBlock(sbinJsonObj.getCDATHexStr(), CDAT_HEADER);
	    cdatBlock.setBlockEmptyBytesCount(sbinJsonObj.getCDATHexEmptyBytesCount());
	    byte[] finalCDAT = buildSBinBlock(cdatBlock);
	    
	    ByteArrayOutputStream fileOutputStr = new ByteArrayOutputStream();
	    fileOutputStr.write(SBIN_HEADER);
	    fileOutputStr.write(BigInteger.valueOf(0x03000000).toByteArray()); // Version 3
	    fileOutputStr.write(finalENUM);
	    fileOutputStr.write(finalSTRU);
	    fileOutputStr.write(finalFIEL);
	    fileOutputStr.write(finalOHDR);
	    fileOutputStr.write(finalDATA);
	    fileOutputStr.write(finalCHDR);
	    fileOutputStr.write(finalCDAT);
	    byte[] fileBytes = fileOutputStr.toByteArray();
	    
	    Files.write(Paths.get("new_" + sbinJsonObj.getFileName()), fileBytes);
	}
	
	public void getFNVHash(String filePath, String type) throws IOException {
		byte[] fileArray = Files.readAllBytes(Paths.get(filePath));
		byte[] filePart;
		
		if (type.contentEquals("dds")) {
			filePart = Arrays.copyOfRange(fileArray, 128, fileArray.length);
		} else {
			filePart = Arrays.copyOfRange(fileArray, 0, fileArray.length);
		}
		
		int hexHash = FNV1.hash32(filePart);
		String output = Integer.toHexString(hexRev(hexHash)).toUpperCase();
		String length = Integer.toHexString(hexRev(filePart.length)).toUpperCase();
		System.out.println("FNV1 Hash: " + output);
		System.out.println("Block Length: " + length);
	}
	
	//
	
	private SBinBlockObj processSBinBlock(byte[] sbinData, byte[] header, byte[] nextHeader) {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		changeCurPos(0x4); // Skip header
		block.setBlockSize(getBytesFromCurPos(sbinData, 0x4));
		block.setBlockSizeInt(byteArrayToInt(block.getBlockSize()));
		changeCurPos(0x8); // Skip size + hash
		if (block.getBlockSizeInt() != 0x0) {
			block.setBlockBytes(getBytesFromCurPos(sbinData, block.getBlockSizeInt()));
			changeCurPos(block.getBlockSizeInt()); 
		}
		
		byte[] fielHeaderCheck = getBytesFromCurPos(sbinData, 0x4);
		while (nextHeader != null && !Arrays.equals(fielHeaderCheck, nextHeader)) {
			changeCurPos(0x1); // Happens in some files and usually on STRU block, with additional empty 0x2 over the block size
			block.setBlockEmptyBytesCount(block.getBlockEmptyBytesCount() + 1);
			fielHeaderCheck = getBytesFromCurPos(sbinData, 0x4);
		}
		
		return block;
	}
	
	private SBinBlockObj createSBinBlock(String hexStr, byte[] header) {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		block.setBlockBytes(decodeHexStr(hexStr));
		block.setFnv1Hash(intToByteArrayBE(FNV1.hash32(block.getBlockBytes()), 0x4)); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(intToByteArrayBE(block.getBlockBytes().length, 0x4));
		}
		return block;
	}
	
	private byte[] buildSBinBlock(SBinBlockObj block) throws IOException {
		ByteArrayOutputStream enumStream = new ByteArrayOutputStream();
	    enumStream.write(block.getHeader());
	    enumStream.write(block.getBlockSize());
	    enumStream.write(block.getFnv1Hash());
	    enumStream.write(block.getBlockBytes());
	    if (block.getBlockEmptyBytesCount() != 0) {
	    	enumStream.write(new byte[block.getBlockEmptyBytesCount()]);
	    }
	    return enumStream.toByteArray();
	}
	
	//
	
	private static void changeCurPos(int addition) {
		curPos = curPos + addition;
		System.out.println("### curPos: " + Integer.toHexString(curPos));
	}
	
	// Taken from StackOverflow (Dhaval Rami)
	public static int byteArrayToInt(byte[] b) {
		final ByteBuffer bb = ByteBuffer.wrap(b);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
	}
	
	private byte[] getBytesFromCurPos(byte[] data, int to) {
		return Arrays.copyOfRange(data, curPos, curPos + to);
	}
	
	private byte[] intToByteArrayBE(int data, int size) {    
		return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(data).array(); 
	}
	
	// Taken from StackOverflow (maybeWeCouldStealAVan)
	private String hexToString(byte[] bytes) {
		byte[] hexChars = new byte[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars, StandardCharsets.UTF_8);
	}
	
	private byte[] decodeHexStr(String str) {
		int len = str.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4)
					+ Character.digit(str.charAt(i+1), 16));
		}
		return data;
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
	
}
