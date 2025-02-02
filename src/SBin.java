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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import util.FNV1;
import util.SBinType;
import util.DataClasses.*;

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
	//
	private static final byte[] SHORTBYTE_EMPTY = decodeHexStr("0000");
	private static final int DDS_HEADER_SIZE = 0x80;
	//
	private static final byte[] BULK_HEADER = "BULK".getBytes(StandardCharsets.UTF_8);
	private static final byte[] BARG_HEADER = "BARG".getBytes(StandardCharsets.UTF_8);
	//
	private static final int DATA_COMMONBYTES = 0x24;
	private static final int CAREER_DATA_COMMONBYTES = 0x80;
	private static final int CAREER_OHDR_COMMONBYTES = 0x2C;
	private static final int CAREER_OHDR_GARAGECARS_BASEVALUE = 0x430;
	private static final byte[] CAREER_DATA_P2_SHORTBYTES = decodeHexStr("0300");
	
	private static int curPos = 0x0;
	
	//

	public void unpackSBin(String fileType, String filePath) throws IOException {
		SBinJson sbinJson = new SBinJson();
		Path sbinFilePath = Paths.get(filePath);
		byte[] sbinData = Files.readAllBytes(sbinFilePath);

		int sbinVersion = sbinData[4];
		if (sbinVersion != 0x03) {
			System.out.println("!!! This SBin version is not supported, version 3 required.");
			return;
		}
		sbinJson.setSBinType(SBinType.valueOf(fileType.toUpperCase()));
		sbinJson.setFileName(sbinFilePath.getFileName().toString());
		sbinJson.setSbinVersion(sbinVersion);
		
		changeCurPos(0x8); // Skip SBin header + version

		// ENUM (Not enough info)
		SBinBlockObj enumBlock = processSBinBlock(sbinData, ENUM_HEADER, STRU_HEADER);		
		sbinJson.setENUMHexStr(hexToString(enumBlock.getBlockBytes()).toUpperCase());
		sbinJson.setENUMHexEmptyBytesCount(Long.valueOf(enumBlock.getBlockEmptyBytesCount()));
		// STRU (Not enough info)
		SBinBlockObj struBlock = processSBinBlock(sbinData, STRU_HEADER, FIEL_HEADER);
		sbinJson.setSTRUHexStr(hexToString(struBlock.getBlockBytes()).toUpperCase());
		sbinJson.setSTRUHexEmptyBytesCount(Long.valueOf(struBlock.getBlockEmptyBytesCount()));
		// FIEL (Not enough info)
		SBinBlockObj fielBlock = processSBinBlock(sbinData, FIEL_HEADER, OHDR_HEADER);	
		sbinJson.setFIELHexStr(hexToString(fielBlock.getBlockBytes()).toUpperCase());
		sbinJson.setFIELHexEmptyBytesCount(Long.valueOf(fielBlock.getBlockEmptyBytesCount()));
		// OHDR (Not enough info)
		SBinBlockObj ohdrBlock = processSBinBlock(sbinData, OHDR_HEADER, DATA_HEADER);		
		saveOHDRBlockData(sbinJson, ohdrBlock);
		// DATA (Partial info)
		SBinBlockObj dataBlock = processSBinBlock(sbinData, DATA_HEADER, CHDR_HEADER);	
		sbinJson.setDATAHexStr(hexToString(dataBlock.getBlockBytes()).toUpperCase());
		sbinJson.setDATAHexEmptyBytesCount(Long.valueOf(dataBlock.getBlockEmptyBytesCount()));
		// CHDR
		SBinBlockObj chdrBlock = processSBinBlock(sbinData, CHDR_HEADER, CDAT_HEADER);
//		sbinJson.setCHDRHexStr(hexToString(chdrBlock.getBlockBytes()).toUpperCase());
		sbinJson.setCHDRHexEmptyBytesCount(Long.valueOf(chdrBlock.getBlockEmptyBytesCount()));
		
		byte[] headerAfterCDAT = null;
		if (sbinJson.getSBinType() == SBinType.TEXTURE) {
			headerAfterCDAT = BULK_HEADER;
		}
		// CDAT
		SBinBlockObj cdatBlock = processSBinBlock(sbinData, CDAT_HEADER, headerAfterCDAT);
//		sbinJson.setCDATHexStr(hexToString(cdatBlock.getBlockBytes()).toUpperCase());
		sbinJson.setCDATHexEmptyBytesCount(Long.valueOf(cdatBlock.getBlockEmptyBytesCount()));
		sbinJson.setCDATStrings(prepareCDATStrings(chdrBlock.getBlockBytes(), cdatBlock.getBlockBytes()));
		
		if (sbinJson.getSBinType() == SBinType.STRINGDATA) {
			unpackStringData(sbinJson, dataBlock);
		} else if (sbinJson.getSBinType() == SBinType.CAREER) {
			unpackCareerData(sbinJson, dataBlock);
		} else if (sbinJson.getSBinType() == SBinType.TEXTURE) {
			// BULK (Partial info)
			SBinBlockObj bulkBlock = processSBinBlock(sbinData, BULK_HEADER, BARG_HEADER);
			sbinJson.setBULKHexStr(hexToString(bulkBlock.getBlockBytes()).toUpperCase());
			sbinJson.setBULKHexEmptyBytesCount(Long.valueOf(bulkBlock.getBlockEmptyBytesCount()));
			// BARG (Image data)
			SBinBlockObj bargBlock = processSBinBlock(sbinData, BARG_HEADER, null);
			primitiveExtractDDS(sbinJson, bargBlock.getBlockBytes());
//			sbinJson.setBARGHexStr(hexToString(bargBlock.getBlockBytes()).toUpperCase());
			sbinJson.setBARGHexEmptyBytesCount(Long.valueOf(bargBlock.getBlockEmptyBytesCount()));
		}
		
		gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonOut = gson.toJson(sbinJson);
		Files.write(Paths.get(sbinJson.getFileName() + ".json"), jsonOut.getBytes(StandardCharsets.UTF_8));
	}

	public void repackSBin(String filePath) throws IOException {
		Reader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
		SBinJson sbinJsonObj = new Gson().fromJson(reader, new TypeToken<SBinJson>(){}.getType());
		reader.close();
		sbinJsonObj.setSBinType(sbinJsonObj.getSBinType());

		// ENUM (Not enough info)
		SBinBlockObj enumBlock = createSBinBlock(sbinJsonObj.getENUMHexStr(), ENUM_HEADER);
		enumBlock.setBlockEmptyBytesCount(sbinJsonObj.getENUMHexEmptyBytesCount().intValue());
		byte[] finalENUM = buildSBinBlock(enumBlock);
		// STRU (Not enough info)
		SBinBlockObj struBlock = createSBinBlock(sbinJsonObj.getSTRUHexStr(), STRU_HEADER);
		struBlock.setBlockEmptyBytesCount(sbinJsonObj.getSTRUHexEmptyBytesCount().intValue());
		byte[] finalSTRU = buildSBinBlock(struBlock);
		// FIEL (Not enough info)
		SBinBlockObj fielBlock = createSBinBlock(sbinJsonObj.getFIELHexStr(), FIEL_HEADER);
		fielBlock.setBlockEmptyBytesCount(sbinJsonObj.getFIELHexEmptyBytesCount().intValue());
		byte[] finalFIEL = buildSBinBlock(fielBlock);
		// OHDR (Not enough info)
		SBinBlockObj ohdrBlock = createOHDRBlock(sbinJsonObj, OHDR_HEADER);
		ohdrBlock.setBlockEmptyBytesCount(sbinJsonObj.getOHDRHexEmptyBytesCount().intValue());
		byte[] finalOHDR = buildSBinBlock(ohdrBlock);
		// DATA (Partial info)
		SBinBlockObj dataBlock = createDATABlock(sbinJsonObj, DATA_HEADER);
		dataBlock.setBlockEmptyBytesCount(sbinJsonObj.getDATAHexEmptyBytesCount().intValue());
		byte[] finalDATA = buildSBinBlock(dataBlock);
		// CHDR & CDAT
		SBinBlockObj cdatBlock = createCDATBlock(sbinJsonObj, CDAT_HEADER);
		cdatBlock.setBlockEmptyBytesCount(sbinJsonObj.getCDATHexEmptyBytesCount().intValue());
		byte[] finalCDAT = buildSBinBlock(cdatBlock);
		//
		SBinBlockObj chdrBlock = createCHDRBlock(cdatBlock.getBlockElements(), CHDR_HEADER);
		chdrBlock.setBlockEmptyBytesCount(sbinJsonObj.getCHDRHexEmptyBytesCount().intValue());
		byte[] finalCHDR = buildSBinBlock(chdrBlock);
		
		ByteArrayOutputStream additionalBlocksStream = new ByteArrayOutputStream();
		if (sbinJsonObj.getSBinType() == SBinType.TEXTURE) {
			// BULK (Partial info)
			SBinBlockObj bulkBlock = createSBinBlock(sbinJsonObj.getBULKHexStr(), BULK_HEADER);
			bulkBlock.setBlockEmptyBytesCount(sbinJsonObj.getBULKHexEmptyBytesCount().intValue());
			byte[] finalBULK = buildSBinBlock(bulkBlock);
			additionalBlocksStream.write(finalBULK);
			// BARG (Image data)
			SBinBlockObj bargBlock = createBARGBlock(sbinJsonObj, BARG_HEADER);
			bargBlock.setBlockEmptyBytesCount(sbinJsonObj.getBARGHexEmptyBytesCount().intValue());
			byte[] finalBARG = buildSBinBlock(bargBlock);
			additionalBlocksStream.write(finalBARG);
		}

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
		fileOutputStr.write(additionalBlocksStream.toByteArray());
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
	// SBin block methods
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
		block.setFnv1Hash(intToByteArrayLE(FNV1.hash32(block.getBlockBytes()), 0x4)); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(intToByteArrayLE(block.getBlockBytes().length, 0x4));
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
	
	private void saveOHDRBlockData(SBinJson sbinJson, SBinBlockObj ohdrBlock) {
		if (sbinJson.getSBinType() == SBinType.CAREER) {
			ohdrBlock.setBlockBytes(Arrays.copyOfRange(
					ohdrBlock.getBlockBytes(), 0x0, CAREER_OHDR_COMMONBYTES));
			sbinJson.setOHDRHexStr(hexToString(ohdrBlock.getBlockBytes()).toUpperCase());
		}
		sbinJson.setOHDRHexStr(hexToString(ohdrBlock.getBlockBytes()).toUpperCase());
		sbinJson.setOHDRHexEmptyBytesCount(Long.valueOf(ohdrBlock.getBlockEmptyBytesCount()));
	}
	
	//
	// SBin unpack functions
	//
	
	private List<String> prepareCDATStrings(byte[] chdrBytes, byte[] cdatBytes) {
		List<byte[]> chdrEntries = splitByteArray(chdrBytes, 0x8);
		List<String> cdatStrings = new ArrayList<>();
		for (byte[] chdrEntry : chdrEntries) {
			int cdatPos = byteArrayToInt(Arrays.copyOfRange(chdrEntry, 0, 4));
			int cdatEntrySize = byteArrayToInt(Arrays.copyOfRange(chdrEntry, 4, 8));
//			System.out.println("### cdatPos: " + Integer.toHexString(cdatPos) + ", cdatSize: " + Integer.toHexString(cdatEntrySize));
			String cdatString = new String(Arrays.copyOfRange(cdatBytes, cdatPos, cdatPos + cdatEntrySize), StandardCharsets.UTF_8);
			cdatStrings.add(cdatString);
		}
		return cdatStrings;
	}
	
	private SBinBlockObj createCDATBlock(SBinJson sbinJson, byte[] header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		
		ByteArrayOutputStream stringsHexStream = new ByteArrayOutputStream();
		List<byte[]> blockElements = new ArrayList<>();
		// Some elements could be empty, like the first one. Then data bytes begins with 00 splitter byte
		addElementsToByteArraysList(sbinJson.getCDATStrings(), blockElements);
		
		if (sbinJson.getSBinType() == SBinType.STRINGDATA) {
			for (SBinStringDataEntry strDataEntry : sbinJson.getStrDataEntriesArray()) {
				String insert = strDataEntry.isTextEntry() ? strDataEntry.getTextValue() : strDataEntry.getStringId();
				byte[] byteEntry = insert.getBytes(StandardCharsets.UTF_8);
				blockElements.add(byteEntry);
			}
		} else if (sbinJson.getSBinType() == SBinType.CAREER) {
			addElementsToByteArraysList(sbinJson.getCareerGarageCarsArray(), blockElements);
		}
		
		block.setBlockElements(blockElements); // Save it for CHDR block
		for (byte[] element : blockElements) {
			stringsHexStream.write(element);
			stringsHexStream.write(new byte[1]); // Zero byte splitter after each entry. Also file ends with empty zero byte
		}
		block.setBlockBytes(stringsHexStream.toByteArray());
		
		block.setFnv1Hash(intToByteArrayLE(FNV1.hash32(block.getBlockBytes()), 0x4)); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(intToByteArrayLE(block.getBlockBytes().length, 0x4));
		}
		return block;
	}
	
	private void unpackStringData(SBinJson sbinJson, SBinBlockObj dataBlock) {
		List<SBinStringDataEntry> strDataEntriesArray = new ArrayList<>();
		
		setCurPos(DATA_COMMONBYTES); // Skip common bytes on DATA
		int stringDataEntriesCount = byteArrayToInt(getBytesFromCurPos(dataBlock.getBlockBytes(), 0x4));
		changeCurPos(0x4);
		byte[] stringDataBytes = getBytesFromCurPos(dataBlock.getBlockBytes(), stringDataEntriesCount * 8);
		
		// Save first unknown DATA bytes
		dataBlock.setBlockBytes(Arrays.copyOfRange(dataBlock.getBlockBytes(), 0, 0x24));
		sbinJson.setDATAHexStr(hexToString(dataBlock.getBlockBytes()).toUpperCase());
		
		int prevTextChdrId = 0; // Prevent Text duplicates
		List<byte[]> splitStringDataBytes = splitByteArray(stringDataBytes, 0x8);
		int firstStringChdrId = twoLEByteArrayToInt(Arrays.copyOfRange(splitStringDataBytes.get(0), 0, 2));
		//
		for (byte[] strDataByteEntry : splitStringDataBytes) {
			int stringChdrId = twoLEByteArrayToInt(Arrays.copyOfRange(strDataByteEntry, 0, 2));
			int textChdrId = twoLEByteArrayToInt(Arrays.copyOfRange(strDataByteEntry, 2, 4));
			String unkHexValue = hexToString(Arrays.copyOfRange(strDataByteEntry, 4, 8)).toUpperCase();
			
			// Create StringData entry. One Text entry can be referenced by more than 1 String entry
			SBinStringDataEntry stringEntry = new SBinStringDataEntry();
			stringEntry.setTextEntry(false);
			stringEntry.setCHDRId(Long.valueOf(stringChdrId));
			stringEntry.setStringId((sbinJson.getCDATStrings().get(stringChdrId)));
			stringEntry.setCHDRIdTextRef(Long.valueOf(textChdrId));
			stringEntry.setHalVersionValue(unkHexValue);
			strDataEntriesArray.add(stringEntry);
			
			// Create Text entry
			if (prevTextChdrId < textChdrId) {
				SBinStringDataEntry textEntry = new SBinStringDataEntry();
				textEntry.setTextEntry(true);
				textEntry.setCHDRId(Long.valueOf(textChdrId));
				textEntry.setCHDRIdTextRef(null); // Use Long here to hide empty TextIdRef for Json output
				textEntry.setTextValue(sbinJson.getCDATStrings().get(textChdrId));
				strDataEntriesArray.add(textEntry);
				prevTextChdrId = textChdrId;
			}
		}
		sbinJson.setStrDataEntriesArray(strDataEntriesArray);
		
		// Keep only structure-related strings in CDAT output
		sbinJson.setCDATStrings(sbinJson.getCDATStrings().subList(0, firstStringChdrId));
	}
	
	private void unpackCareerData(SBinJson sbinJson, SBinBlockObj dataBlock) {
		// Skip garage cars count
		sbinJson.setCareerFirstDATAByteValue(hexToString(Arrays.copyOfRange(
				dataBlock.getBlockBytes(), CAREER_DATA_COMMONBYTES + 0x4, CAREER_DATA_COMMONBYTES + 0x8)).toUpperCase());
		setCurPos(CAREER_DATA_COMMONBYTES); // Skip common bytes on DATA
		int careerGarageEntriesCount = byteArrayToInt(getBytesFromCurPos(dataBlock.getBlockBytes(), 0x4));
		// Get over the first DATA part with cars, and skip the first empty bytes
		changeCurPos(0x4 + (careerGarageEntriesCount * 4) + 0x4);
		int firstCarStringId = twoLEByteArrayToInt(getBytesFromCurPos(dataBlock.getBlockBytes(), 0x2));
		
		List<String> careerGarageCarsArray = sbinJson.getCDATStrings().subList(firstCarStringId, sbinJson.getCDATStrings().size());
		sbinJson.setCareerGarageCarsArray(careerGarageCarsArray);
		
		// Cut already processed byte data
		dataBlock.setBlockBytes(Arrays.copyOfRange(
				dataBlock.getBlockBytes(), 0x0, CAREER_DATA_COMMONBYTES));
		sbinJson.setDATAHexStr(hexToString(dataBlock.getBlockBytes()).toUpperCase());
		sbinJson.setCDATStrings(sbinJson.getCDATStrings().subList(0, firstCarStringId));
	}
	
	private void primitiveExtractDDS(SBinJson sbinJson, byte[] imageHex) throws IOException {
		ByteArrayOutputStream ddsFileStream = new ByteArrayOutputStream();
		byte[] headerHex = Files.readAllBytes(Paths.get("templates/dds_1024x_headertemplate"));
		ddsFileStream.write(headerHex);
		ddsFileStream.write(imageHex);
		Files.write(Paths.get(sbinJson.getFileName() + ".dds"), ddsFileStream.toByteArray());
	}
	
	//
	// SBin repack functions
	//
	
	private SBinBlockObj createCHDRBlock(List<byte[]> cdatElements, byte[] header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		
		ByteArrayOutputStream chdrHexStream = new ByteArrayOutputStream();
		int pos = 0;
		for (byte[] cdatEntry : cdatElements) {
			int chdrToCdatPos = pos;
			int chdrToCdatSize = cdatEntry.length;
			pos = pos + chdrToCdatSize + 0x1; // Zero byte-splitter
			chdrHexStream.write(intToByteArrayLE(chdrToCdatPos, 0x4));
			chdrHexStream.write(intToByteArrayLE(chdrToCdatSize, 0x4));
		}
		block.setBlockBytes(chdrHexStream.toByteArray());
		
		block.setFnv1Hash(intToByteArrayLE(FNV1.hash32(block.getBlockBytes()), 0x4)); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(intToByteArrayLE(block.getBlockBytes().length, 0x4));
		}
		return block;
	}
	
	private SBinBlockObj createOHDRBlock(SBinJson sbinJson, byte[] header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		
		ByteArrayOutputStream ohdrHexStream = new ByteArrayOutputStream();
		ohdrHexStream.write(decodeHexStr(sbinJson.getOHDRHexStr()));
		if (sbinJson.getSBinType() == SBinType.CAREER) {
			prepareCareerOHDRForSBinBlock(ohdrHexStream, sbinJson.getCareerGarageCarsArray().size());
		}
		block.setBlockBytes(ohdrHexStream.toByteArray());
		
		block.setFnv1Hash(intToByteArrayLE(FNV1.hash32(block.getBlockBytes()), 0x4)); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(intToByteArrayLE(block.getBlockBytes().length, 0x4));
		}
		return block;
	}
	
	private SBinBlockObj createDATABlock(SBinJson sbinJson, byte[] header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		
		ByteArrayOutputStream dataHexStream = new ByteArrayOutputStream();
		dataHexStream.write(decodeHexStr(sbinJson.getDATAHexStr()));
		if (sbinJson.getSBinType() == SBinType.STRINGDATA) {
			prepareStringDataForSBinBlock(dataHexStream, sbinJson.getStrDataEntriesArray());
		} else if (sbinJson.getSBinType() == SBinType.CAREER) {
			prepareCareerDataForSBinBlock(dataHexStream, sbinJson);
		}
		block.setBlockBytes(dataHexStream.toByteArray());
		
		block.setFnv1Hash(intToByteArrayLE(FNV1.hash32(block.getBlockBytes()), 0x4)); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(intToByteArrayLE(block.getBlockBytes().length, 0x4));
		}
		return block;
	}
	
	// Very primitive
	private SBinBlockObj createBARGBlock(SBinJson sbinJson, byte[] header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		
		byte[] imageHex = Files.readAllBytes(Paths.get(sbinJson.getFileName() + ".dds"));
		block.setBlockBytes(Arrays.copyOfRange(imageHex, DDS_HEADER_SIZE, imageHex.length));
		
		block.setFnv1Hash(intToByteArrayLE(FNV1.hash32(block.getBlockBytes()), 0x4)); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(intToByteArrayLE(block.getBlockBytes().length, 0x4));
		}
		return block;
	}
	
	private void prepareStringDataForSBinBlock(ByteArrayOutputStream dataHexStream, List<SBinStringDataEntry> entries) throws IOException {
		ByteArrayOutputStream strDataHexStream = new ByteArrayOutputStream();
		int dataStrCount = 0;
		
		for (SBinStringDataEntry entry : entries) {
			if (entry.isTextEntry()) {continue;}
			dataStrCount++;
			strDataHexStream.write(shortToBytes((short)entry.getCHDRId().intValue()));
			strDataHexStream.write(shortToBytes((short)entry.getCHDRIdTextRef().intValue()));
			strDataHexStream.write(decodeHexStr(entry.getHalVersionValue()));
		}
		dataHexStream.write(intToByteArrayLE(dataStrCount, 0x4)); // StringData entries count, without Text entries
		dataHexStream.write(strDataHexStream.toByteArray());
	}
	
	private void prepareCareerOHDRForSBinBlock(ByteArrayOutputStream ohdrHexStream, int careerGarageCarsCount) throws IOException {
		for (int i = 0; i < careerGarageCarsCount; i++) {
			ohdrHexStream.write(intToByteArrayLE(
					CAREER_OHDR_GARAGECARS_BASEVALUE + (0x20 * careerGarageCarsCount) + (0x20 * i), 0x4));
		}
		
	}
	
	private void prepareCareerDataForSBinBlock(ByteArrayOutputStream dataHexStream, SBinJson sbinJson) throws IOException {
		ByteArrayOutputStream careerData1HexStream = new ByteArrayOutputStream();
		ByteArrayOutputStream careerData2HexStream = new ByteArrayOutputStream();
		int careerCarsGarageCount = sbinJson.getCareerGarageCarsArray().size();
		careerData1HexStream.write(intToByteArrayLE(careerCarsGarageCount, 0x4));
		// First value in part 2 block
		careerData2HexStream.write(SHORTBYTE_EMPTY); 
		careerData2HexStream.write(CAREER_DATA_P2_SHORTBYTES);
		
		int minimalCareerDataP1Value = byteArrayToInt(decodeHexStr(sbinJson.getCareerFirstDATAByteValue()));
		int minimalCareerDataP2Value = sbinJson.getCDATStrings().size();
		
		for (int i = 0; i < sbinJson.getCareerGarageCarsArray().size(); i++) {
			careerData1HexStream.write(intToByteArrayLE(minimalCareerDataP1Value + i, 0x4));
			careerData2HexStream.write(shortToBytes(minimalCareerDataP2Value + i));
			if (i + 1 < sbinJson.getCareerGarageCarsArray().size()) {
				careerData2HexStream.write(CAREER_DATA_P2_SHORTBYTES);
			} else { // Last one must be empty
				careerData2HexStream.write(SHORTBYTE_EMPTY);
			}
		}
		dataHexStream.write(careerData1HexStream.toByteArray());
		dataHexStream.write(careerData2HexStream.toByteArray());
	}

	//
	// Util methods
	//

	private static void changeCurPos(int addition) {
		curPos = curPos + addition;
//		System.out.println("### curPos: " + Integer.toHexString(curPos));
	}
	
	private static void setCurPos(int newPos) {
		curPos = newPos;
//		System.out.println("### curPos: " + Integer.toHexString(curPos));
	}
	
	private static void addElementsToByteArraysList(List<String> list, List<byte[]> blockElements) {
		for (String entry : list) {
			blockElements.add(entry.getBytes(StandardCharsets.UTF_8));
		}
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

	private byte[] getBytesFromCurPos(byte[] data, int to) {
		return Arrays.copyOfRange(data, curPos, curPos + to);
	}

	private byte[] intToByteArrayLE(int data, int size) {    
		return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(data).array(); 
	}
	
	private byte[] shortToBytes(int data) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short)data).array();
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

	private static byte[] decodeHexStr(String str) {
		int len = str.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4)
					+ Character.digit(str.charAt(i+1), 16));
		}
		return data;
	}
	
	// https://stackoverflow.com/a/66638297
	public List<byte[]> splitByteArray(byte[] array, int chunkSize) {
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
	
}
