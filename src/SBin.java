import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
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
import util.HEXClasses.*;
import util.HEXUtils;
import util.SBinEnumUtils;
import util.SBinFieldType;

public class SBin {
	
	Gson gson = new Gson();
	
	private static final byte[] SBIN_HEADER = "SBIN".getBytes(StandardCharsets.UTF_8);
	private static final byte[] ENUM_HEADER = "ENUM".getBytes(StandardCharsets.UTF_8);
	private static final byte[] STRU_HEADER = "STRU".getBytes(StandardCharsets.UTF_8);
	private static final byte[] FIEL_HEADER = "FIEL".getBytes(StandardCharsets.UTF_8);
	private static final byte[] OHDR_HEADER = "OHDR".getBytes(StandardCharsets.UTF_8);
	private static final byte[] DATA_HEADER = "DATA".getBytes(StandardCharsets.UTF_8);
	private static final byte[] CHDR_HEADER = "CHDR".getBytes(StandardCharsets.UTF_8);
	private static final byte[] CDAT_HEADER = "CDAT".getBytes(StandardCharsets.UTF_8);
	//
	private static final byte[] SHORTBYTE_EMPTY = new byte[2];
	private static final int DDS_HEADER_SIZE = 0x80;
	//
	private static final byte[] BULK_HEADER = "BULK".getBytes(StandardCharsets.UTF_8);
	private static final byte[] BARG_HEADER = "BARG".getBytes(StandardCharsets.UTF_8);
	//
	private static final int DATA_COMMONBYTES = 0x24;
	private static final int CAREER_DATA_COMMONBYTES = 0x80;
	private static final int CAREER_OHDR_COMMONBYTES = 0x2C;
	private static final int CAREER_OHDR_GARAGECARS_BASEVALUE = 0x430;
	private static final byte[] CAREER_DATA_P2_SHORTBYTES = HEXUtils.decodeHexStr("0300");
	//
	private static final byte[] PLAYLISTS_DATA_DESC_UNK1 = HEXUtils.decodeHexStr("1C0002000D000C000000");
	private static final byte[] PLAYLISTS_DATA_DESC_UNK2 = HEXUtils.decodeHexStr("04000F00180000000000");
	private static final byte[] PLAYLISTS_DATA_TRACK_UNK1 = HEXUtils.decodeHexStr("220005000D000C000000");
	private static final byte[] PLAYLISTS_DATA_TRACK_UNK2 = HEXUtils.decodeHexStr("07000D0016000000");
	private static final byte[] PLAYLISTS_DATA_TRACK_UNK3 = HEXUtils.decodeHexStr("08000D0020000000");
	
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
		sbinJson.setENUMHexStr(HEXUtils.hexToString(enumBlock.getBlockBytes()).toUpperCase());
		sbinJson.setENUMHexEmptyBytesCount(Long.valueOf(enumBlock.getBlockEmptyBytesCount()));
		// STRU: object structures of DATA block
		SBinBlockObj struBlock = processSBinBlock(sbinData, STRU_HEADER, FIEL_HEADER);
		sbinJson.setSTRUHexEmptyBytesCount(Long.valueOf(struBlock.getBlockEmptyBytesCount()));
		// FIEL: info fields for Structs
		SBinBlockObj fielBlock = processSBinBlock(sbinData, FIEL_HEADER, OHDR_HEADER);	
		sbinJson.setFIELHexEmptyBytesCount(Long.valueOf(fielBlock.getBlockEmptyBytesCount()));
		//
		if (sbinJson.getSBinType() != SBinType.COMMON) {
			sbinJson.setSTRUHexStr(HEXUtils.hexToString(struBlock.getBlockBytes()).toUpperCase());
			sbinJson.setFIELHexStr(HEXUtils.hexToString(fielBlock.getBlockBytes()).toUpperCase());
		}
		// OHDR: map of DATA block
		SBinBlockObj ohdrBlock = processSBinBlock(sbinData, OHDR_HEADER, DATA_HEADER);		
		saveOHDRBlockData(sbinJson, ohdrBlock);
		// DATA: various objects info
		SBinBlockObj dataBlock = processSBinBlock(sbinData, DATA_HEADER, CHDR_HEADER);	
		sbinJson.setDATAHexEmptyBytesCount(Long.valueOf(dataBlock.getBlockEmptyBytesCount()));
		// CHDR: map of CDAT block
		SBinBlockObj chdrBlock = processSBinBlock(sbinData, CHDR_HEADER, CDAT_HEADER);
		sbinJson.setCHDRHexEmptyBytesCount(Long.valueOf(chdrBlock.getBlockEmptyBytesCount()));
		// CDAT: field names & string variables
		SBinBlockObj cdatBlock = processSBinBlock(sbinData, CDAT_HEADER, 
				sbinJson.getSBinType() == SBinType.TEXTURE ? BULK_HEADER : null);
		sbinJson.setCDATHexEmptyBytesCount(Long.valueOf(cdatBlock.getBlockEmptyBytesCount()));
		sbinJson.setCDATStrings(prepareCDATStrings(chdrBlock.getBlockBytes(), cdatBlock.getBlockBytes()));
		
		readStructsAndFields(sbinJson, struBlock, fielBlock);
		parseDATABlock(sbinJson, ohdrBlock, dataBlock);
		switch(sbinJson.getSBinType()) {
		case STRINGDATA:
			unpackStringData(sbinJson, dataBlock);
			break;
		case CAREER:
			unpackCareerData(sbinJson, dataBlock);
			break;
		case ACHIEVEMENTS:
			unpackAchievementsData(sbinJson, dataBlock);
			break;
		case PLAYLISTS:
			unpackPlaylistsData(sbinJson, dataBlock);
			break;
		case TEXTURE:
			// BULK (Partial info)
			SBinBlockObj bulkBlock = processSBinBlock(sbinData, BULK_HEADER, BARG_HEADER);
			sbinJson.setBULKHexStr(HEXUtils.hexToString(bulkBlock.getBlockBytes()).toUpperCase());
			sbinJson.setBULKHexEmptyBytesCount(Long.valueOf(bulkBlock.getBlockEmptyBytesCount()));
			// BARG (Image data)
			SBinBlockObj bargBlock = processSBinBlock(sbinData, BARG_HEADER, null);
			primitiveExtractDDS(sbinJson, bargBlock.getBlockBytes());
			sbinJson.setBARGHexEmptyBytesCount(Long.valueOf(bargBlock.getBlockEmptyBytesCount()));
			break;
		default: break;
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
		// DATA & OHDR
		SBinBlockObj dataBlock = createDATABlock(sbinJsonObj, DATA_HEADER);
		dataBlock.setBlockEmptyBytesCount(sbinJsonObj.getDATAHexEmptyBytesCount().intValue());
		byte[] finalDATA = buildSBinBlock(dataBlock);
		//
		SBinBlockObj ohdrBlock = createOHDRBlock(sbinJsonObj, dataBlock, OHDR_HEADER);
		ohdrBlock.setBlockEmptyBytesCount(sbinJsonObj.getOHDRHexEmptyBytesCount().intValue());
		byte[] finalOHDR = buildSBinBlock(ohdrBlock);
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
		String output = Integer.toHexString(HEXUtils.hexRev(hexHash)).toUpperCase();
		String length = Integer.toHexString(HEXUtils.hexRev(filePart.length)).toUpperCase();
		System.out.println("FNV1 Hash: " + output);
		System.out.println("Block Length: " + length);
	}
	
	// Currently only for CarDesc. Old method
	public void calcOHDR(int swatchCount, int extraValueMode) throws IOException {
		ByteArrayOutputStream ohdrStream = new ByteArrayOutputStream();
		byte[] ohdrStartBytes = Files.readAllBytes(Paths.get("templates/ohdr_cardesc_starttemplate"));
		ohdrStream.write(ohdrStartBytes);
		int addition = extraValueMode != 0 ? 0x20 : 0x0;
		
		HEXUtils.writeBytesWithAddition(ohdrStream, 0x310, addition);
		HEXUtils.writeBytesWithAddition(ohdrStream, 0x4E2, addition);
		
		addition += 0x20 * swatchCount;
		int baseValue = 0x530;
		for (int i = 0; i < swatchCount; i++) {
			if (i > 0) {addition += 0x40;}
			HEXUtils.writeBytesWithAddition(ohdrStream, baseValue, addition);
			addition += 0xE0;
			HEXUtils.writeBytesWithAddition(ohdrStream, baseValue, addition);
			addition += 0x40;
			HEXUtils.writeBytesWithAddition(ohdrStream, baseValue, addition);
		}
		
		addition += 0x32;
		HEXUtils.writeBytesWithAddition(ohdrStream, baseValue, addition);
		//
		addition += 0x14E;
		HEXUtils.writeBytesWithAddition(ohdrStream, baseValue, addition);
		//
		for (int i = 0; i < 8; i++) {
			addition += 0xA0;
			HEXUtils.writeBytesWithAddition(ohdrStream, baseValue, addition);
		}
		if (extraValueMode != 0) {
			addition += 0x40;
			if (extraValueMode == 2) {addition += 0x20;}
			HEXUtils.writeBytesWithAddition(ohdrStream, baseValue, addition);
		}
		System.out.println("OHDR CarDesc block for " + swatchCount + " swatches, extraValueMode " + extraValueMode + ": "
				+ HEXUtils.hexToString(ohdrStream.toByteArray()).toUpperCase());
	}

	//
	// SBin block methods
	//

	private SBinBlockObj processSBinBlock(byte[] sbinData, byte[] header, byte[] nextHeader) {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		changeCurPos(0x4); // Skip header
		block.setBlockSize(getBytesFromCurPos(sbinData, 0x4));
		block.setBlockSizeInt(HEXUtils.byteArrayToInt(block.getBlockSize()));
		changeCurPos(0x8); // Skip size + hash
		if (block.getBlockSizeInt() != 0x0) {
			block.setBlockBytes(getBytesFromCurPos(sbinData, block.getBlockSizeInt()));
			if (Arrays.equals(header, STRU_HEADER)) {
				block.setBlockElements(HEXUtils.splitByteArray(block.getBlockBytes(), 0x6));
			} else if (Arrays.equals(header, FIEL_HEADER)) {
				block.setBlockElements(HEXUtils.splitByteArray(block.getBlockBytes(), 0x8));
			}
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
		block.setBlockBytes(HEXUtils.decodeHexStr(hexStr));
		block.setFnv1Hash(HEXUtils.intToByteArrayLE(FNV1.hash32(block.getBlockBytes()), 0x4)); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(HEXUtils.intToByteArrayLE(block.getBlockBytes().length, 0x4));
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
//			sbinJson.setOHDRHexStr(hexToString(ohdrBlock.getBlockBytes()).toUpperCase());
		}
//		sbinJson.setOHDRHexStr(hexToString(ohdrBlock.getBlockBytes()).toUpperCase());
		sbinJson.setOHDRHexEmptyBytesCount(Long.valueOf(ohdrBlock.getBlockEmptyBytesCount()));
	}
	
	private void setSBinBlockAttributes(SBinBlockObj block) {
		block.setFnv1Hash(HEXUtils.intToByteArrayLE(FNV1.hash32(block.getBlockBytes()), 0x4)); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(HEXUtils.intToByteArrayLE(block.getBlockBytes().length, 0x4));
		}
	}
	
	// Some DATA entries ends with short "00 00" instead of 4 bytes
	private List<byte[]> readDATABlockObjectMap(byte[] map) {
		List<byte[]> entries = new ArrayList<>();
		int entriesCount = HEXUtils.byteArrayToInt(Arrays.copyOfRange(map, 4, 8));
		if (entriesCount != 0) {
			byte[] mapEntries = Arrays.copyOfRange(map, 8, map.length);
			
			for (int i = 0; i < entriesCount; i++) {
				int offset = 4 * i;
				entries.add(Arrays.copyOfRange(mapEntries, offset, 4 + offset));
			}
		}
		return entries;
	}
	
	private void subLast2BytesOHDR(SBinBlockObj block) {
		int index = block.getOHDRMapTemplate().size() - 1;
		SBinOHDREntry lastOHDREntry = block.getOHDRMapTemplate().get(index);
		lastOHDREntry.setValue(lastOHDREntry.getValue() - 0xE);
	}
	
	private String getCDATStringByShortCHDRId(byte[] bytes, int startIndex, int endIndex, List<SBinCDATEntry> cdatStrings) {
		int hexCHDRId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(bytes, startIndex, endIndex));
		return cdatStrings.get(hexCHDRId).getString();
	}
	
	private byte[] processStringInCDAT(List<SBinCDATEntry> cdatList, String string) {
		for (SBinCDATEntry cdatEntry : cdatList) {
			if (cdatEntry.getString().contentEquals(string)) {
				return HEXUtils.decodeHexStr(cdatEntry.getChdrHexId());
			}
		}
		byte[] newCHDRId = HEXUtils.shortToBytes(cdatList.size());
		SBinCDATEntry newEntry = new SBinCDATEntry();
		newEntry.setString(string);
		newEntry.setChdrHexId(HEXUtils.hexToString(newCHDRId));
		cdatList.add(newEntry);
		return newCHDRId;
	}
	
	//
	// SBin unpack functions
	//
	
	private void readStructsAndFields(SBinJson sbinJson, SBinBlockObj struBlock, SBinBlockObj fielBlock) {
		if (sbinJson.getSBinType() != SBinType.COMMON || struBlock.getBlockSizeInt() == 0
				|| fielBlock.getBlockSizeInt() == 0) {return;}
		
		for (byte[] structBytes : struBlock.getBlockElements()) {
			SBinStruct struct = new SBinStruct();
			struct.setName(
					getCDATStringByShortCHDRId(structBytes, 0, 2, sbinJson.getCDATStrings()));
			
			int firstFieldId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(structBytes, 2, 4));
			int fieldsCount = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(structBytes, 4, 6));
			for (int i = 0; i < fieldsCount; i++) {
				byte[] fieldHex = fielBlock.getBlockElements().get(firstFieldId + i);
				SBinField fieldObj = new SBinField();
				fieldObj.setName(
						getCDATStringByShortCHDRId(fieldHex, 0, 2, sbinJson.getCDATStrings()));
				fieldObj.setStartOffset(HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(fieldHex, 4, 6)));
				fieldObj.setUnkOrderId(HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(fieldHex, 6, 8)));
				int type = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(fieldHex, 2, 4));
				
				fieldObj.setFieldTypeEnum(SBinFieldType.valueOf(type));
				String fieldEnumStr;
				if (fieldObj.getFieldTypeEnum() == null) {
					fieldEnumStr = "UNK_" + Integer.toHexString(type);
					struct.setKnownTypes(false); // Do not parse DATA here - unknown entry size
				} else {
					fieldEnumStr = fieldObj.getFieldTypeEnum().toString();
					fieldObj.setFieldSize(getFieldSize(i, fieldsCount, fieldObj.getStartOffset(), fielBlock, firstFieldId));
				}
				fieldObj.setType(fieldEnumStr);
				struct.addToFields(fieldObj);
			}
			
			struct.getFieldsArray().get(struct.getFieldsArray().size() - 1).setDynamicSize(true);
			sbinJson.addStruct(struct);
		}
	}
	
	// Fields can have a different sizes even for the same types, also there is no way to know about the size of last element
	private int getFieldSize(int i, int fieldsCount, int startOffset, SBinBlockObj fielBlock, int firstFieldId) {
		int nextFieldStartOffset = startOffset;
		if (i + 1 < fieldsCount) {
			nextFieldStartOffset = HEXUtils.twoLEByteArrayToInt(
					Arrays.copyOfRange(fielBlock.getBlockElements().get(firstFieldId + i + 1), 4, 6));
		}
		return nextFieldStartOffset - startOffset;
	}
	
	private void parseDATABlock(SBinJson sbinJson, SBinBlockObj ohdrBlock, SBinBlockObj dataBlock) {
		List<byte[]> ohdrPosDataBytes = HEXUtils.splitByteArray(ohdrBlock.getBlockBytes(), 0x4);
		ohdrPosDataBytes.remove(0); // First one is always 0x1
		ohdrPosDataBytes.add(HEXUtils.intToByteArrayLE(dataBlock.getBlockBytes().length * 0x8, 0x4)); // Create a fake last one, same as DATA size
		List<SBinDataElement> sbinDataElements = new ArrayList<>();
		List<byte[]> blockElements = new ArrayList<>();
		
		int ohdrPrevValue = 0;
		int i = 0;
		for (byte[] ohdrPos : ohdrPosDataBytes) {
			SBinDataElement element = new SBinDataElement();
			int elementOHDR = HEXUtils.byteArrayToInt(ohdrPos);
			int elementBegin = elementOHDR / 0x8;
			
			byte[] elementHex = Arrays.copyOfRange(dataBlock.getBlockBytes(), ohdrPrevValue, elementBegin);
			element.setHexValue(HEXUtils.hexToString(elementHex).toUpperCase());
			element.setOrderHexId(HEXUtils.hexToString(HEXUtils.shortToBytes(i)));
			element.setOhdrUnkRemainder(elementOHDR - (elementBegin * 0x8));
			ohdrPrevValue = elementBegin;
			i++;
			
			int structId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 0, 2));
			if (!sbinJson.getStructs().isEmpty() && structId < sbinJson.getStructs().size()) {
				// TODO Ignore map parsing at the moment
				List<SBinDataField> fields = new ArrayList<>();
				SBinStruct struct = sbinJson.getStructs().get(structId);
				if (struct == null || !struct.isKnownTypes()) {continue;}
				element.setStructName(struct.getName());
//				int fieldsTakenSize = 0;
				for (SBinField field : struct.getFieldsArray()) {
					SBinDataField dataField = new SBinDataField();
					dataField.setName(field.getName());
					dataField.setType(field.getType());
					dataField.setValue(getDataFieldValue(field, dataField, elementHex, sbinJson));
					fields.add(dataField);
//					fieldsTakenSize += field.getFieldSize();
				}
//				if (fieldsTakenSize != elementHex.length) {
//					element.setExtraHexValue(HEXUtils.hexToString( // +2 for struct header
//							Arrays.copyOfRange(elementHex, fieldsTakenSize + 2, elementHex.length)));
//				}
				element.setFields(fields);
			}
			sbinDataElements.add(element);
			blockElements.add(elementHex); // For internal use
		}
		sbinJson.setDataElements(sbinDataElements);
		dataBlock.setBlockElements(blockElements);
	}
	
	private String getDataFieldValue(SBinField field, SBinDataField dataField, byte[] elementHex, SBinJson sbinJson) {
		// First two bytes is taken by Struct ID, start byte goes after it
		int startOffset = field.getStartOffset() + 2;
		int fieldRealSize = field.isDynamicSize() ? 
				elementHex.length - startOffset : field.getFieldSize();
		System.out.println("field: " + field.getName() + ", startOffset: " + field.getStartOffset() + ", fieldRealSize: " + fieldRealSize + ", dynamicSize: " + field.isDynamicSize());
		byte[] valueHex = Arrays.copyOfRange(elementHex, startOffset, startOffset + fieldRealSize);
		return SBinEnumUtils.formatFieldValueUnpack(field.getFieldTypeEnum(), dataField, fieldRealSize, valueHex, sbinJson);
	}
	
	private List<SBinCDATEntry> prepareCDATStrings(byte[] chdrBytes, byte[] cdatBytes) {
		List<byte[]> chdrEntries = HEXUtils.splitByteArray(chdrBytes, 0x8);
		List<SBinCDATEntry> cdatStrings = new ArrayList<>();
		int hexId = 0x0;
		for (byte[] chdrEntry : chdrEntries) {
			int cdatPos = HEXUtils.byteArrayToInt(Arrays.copyOfRange(chdrEntry, 0, 4));
			int cdatEntrySize = HEXUtils.byteArrayToInt(Arrays.copyOfRange(chdrEntry, 4, 8));
			//System.out.println("### cdatPos: " + Integer.toHexString(cdatPos) + ", cdatSize: " + Integer.toHexString(cdatEntrySize));
			
			SBinCDATEntry cdatEntry = new SBinCDATEntry();
			cdatEntry.setString(new String(Arrays.copyOfRange(cdatBytes, cdatPos, cdatPos + cdatEntrySize), StandardCharsets.UTF_8));
			cdatEntry.setChdrHexId(HEXUtils.hexToString(HEXUtils.shortToBytes(hexId)));
			cdatStrings.add(cdatEntry);
			hexId++;
		}
		return cdatStrings;
	}
	
	private SBinBlockObj createCDATBlock(SBinJson sbinJson, byte[] header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		
		ByteArrayOutputStream stringsHexStream = new ByteArrayOutputStream();
		List<byte[]> blockElements = new ArrayList<>();
		// Some elements could be empty, like the first one. Then data bytes begins with 00 splitter byte
		HEXUtils.addCDATElementsToByteArraysList(sbinJson.getCDATStrings(), blockElements);
		
		if (sbinJson.getSBinType() == SBinType.STRINGDATA) {
			for (SBinStringDataEntry strDataEntry : sbinJson.getStrDataEntriesArray()) {
				String insert = strDataEntry.isTextEntry() ? strDataEntry.getTextValue() : strDataEntry.getStringId();
				byte[] byteEntry = insert.getBytes(StandardCharsets.UTF_8);
				blockElements.add(byteEntry);
			}
		} else if (sbinJson.getSBinType() == SBinType.CAREER) {
			HEXUtils.addElementsToByteArraysList(sbinJson.getCareerGarageCarsArray(), blockElements);
		}
		
		block.setBlockElements(blockElements); // Save it for CHDR block
		for (byte[] element : blockElements) {
			stringsHexStream.write(element);
			stringsHexStream.write(new byte[1]); // Zero byte splitter after each entry. Also file ends with empty zero byte
		}
		block.setBlockBytes(stringsHexStream.toByteArray());
		
		setSBinBlockAttributes(block);
		return block;
	}
	
	private void unpackStringData(SBinJson sbinJson, SBinBlockObj dataBlock) {
		List<SBinStringDataEntry> strDataEntriesArray = new ArrayList<>();
		
		setCurPos(DATA_COMMONBYTES); // Skip common bytes on DATA
		int stringDataEntriesCount = HEXUtils.byteArrayToInt(getBytesFromCurPos(dataBlock.getBlockBytes(), 0x4));
		changeCurPos(0x4);
		byte[] stringDataBytes = getBytesFromCurPos(dataBlock.getBlockBytes(), stringDataEntriesCount * 8);
		
		// Save the 4 bytes of last DATA entry, other length is taken by the locale info
		SBinDataElement lastUnkDataElement = sbinJson.getDataElements().get(sbinJson.getDataElements().size() - 1);
		lastUnkDataElement.setHexValue(lastUnkDataElement.getHexValue().substring(0, 4 * 2));
		
		int prevTextChdrId = 0; // Prevent Text duplicates
		List<byte[]> splitStringDataBytes = HEXUtils.splitByteArray(stringDataBytes, 0x8);
		int firstStringChdrId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(splitStringDataBytes.get(0), 0, 2));
		//
		for (byte[] strDataByteEntry : splitStringDataBytes) {
			int stringChdrId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(strDataByteEntry, 0, 2));
			int textChdrId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(strDataByteEntry, 2, 4));
			String unkHexValue = HEXUtils.hexToString(Arrays.copyOfRange(strDataByteEntry, 4, 8)).toUpperCase();
			
			// Create StringData entry. One Text entry can be referenced by more than 1 String entry
			SBinStringDataEntry stringEntry = new SBinStringDataEntry();
			stringEntry.setTextEntry(false);
			stringEntry.setCHDRId(Long.valueOf(stringChdrId));
			stringEntry.setStringId((sbinJson.getCDATStrings().get(stringChdrId).getString()));
			stringEntry.setCHDRIdTextRef(Long.valueOf(textChdrId));
			stringEntry.setHalVersionValue(unkHexValue);
			strDataEntriesArray.add(stringEntry);
			
			// Create Text entry
			if (prevTextChdrId < textChdrId) {
				SBinStringDataEntry textEntry = new SBinStringDataEntry();
				textEntry.setTextEntry(true);
				textEntry.setCHDRId(Long.valueOf(textChdrId));
				textEntry.setCHDRIdTextRef(null); // Use Long here to hide empty TextIdRef for Json output
				textEntry.setTextValue(sbinJson.getCDATStrings().get(textChdrId).getString());
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
		sbinJson.setCareerFirstDATAByteValue(HEXUtils.hexToString(Arrays.copyOfRange(
				dataBlock.getBlockBytes(), CAREER_DATA_COMMONBYTES + 0x4, CAREER_DATA_COMMONBYTES + 0x8)).toUpperCase());
		setCurPos(CAREER_DATA_COMMONBYTES); // Skip common bytes on DATA
		int careerGarageEntriesCount = HEXUtils.byteArrayToInt(getBytesFromCurPos(dataBlock.getBlockBytes(), 0x4));
		// Get over the first DATA part with cars, and skip the first empty bytes
		changeCurPos(0x4 + (careerGarageEntriesCount * 4) + 0x4);
		int firstCarStringId = HEXUtils.twoLEByteArrayToInt(getBytesFromCurPos(dataBlock.getBlockBytes(), 0x2));
		
		List<String> cdatStrings = new ArrayList<>();
		for (SBinCDATEntry entry : sbinJson.getCDATStrings()) {
			cdatStrings.add(entry.getString());
		}
		List<String> careerGarageCarsArray = cdatStrings.subList(firstCarStringId, cdatStrings.size());
		sbinJson.setCareerGarageCarsArray(careerGarageCarsArray);
		
		// Cut already processed byte data
		SBinDataElement lastUnkDataElement = sbinJson.getDataElements().get(10);
		lastUnkDataElement.setHexValue(lastUnkDataElement.getHexValue().substring(0, 4 * 2));
		
		sbinJson.setCDATStrings(sbinJson.getCDATStrings().subList(0, firstCarStringId));
	}
	
	private void unpackAchievementsData(SBinJson sbinJson, SBinBlockObj dataBlock) {
		List<SBinAchievementEntry> achievementsArray = new ArrayList<>();
		
		List<byte[]> achievementsMap = readDATABlockObjectMap(dataBlock.getBlockElements().get(8));
		int firstStringCHDRId = 0;
		// First entry is a header, second is int size
		for (byte[] dataId : achievementsMap) {
			int dataIndex = HEXUtils.byteArrayToInt(dataId);
			byte[] achiHex = dataBlock.getBlockElements().get(dataIndex);
			SBinAchievementEntry achievement = new SBinAchievementEntry();
			
			achievement.setOhdrUnkRemainder(sbinJson.getDataElements().get(dataIndex).getOhdrUnkRemainder());
			if (firstStringCHDRId == 0) {
				firstStringCHDRId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(achiHex, 2, 4));
			}
			achievement.setName(
					getCDATStringByShortCHDRId(achiHex, 2, 4, sbinJson.getCDATStrings()));
			achievement.setDesc(
					getCDATStringByShortCHDRId(achiHex, 4, 6, sbinJson.getCDATStrings()));
			achievement.setPointsInt(
					HEXUtils.byteArrayToInt(Arrays.copyOfRange(achiHex, 6, 10)));
			achievement.setAutologAwardId(
					getCDATStringByShortCHDRId(achiHex, 10, 12, sbinJson.getCDATStrings()));
			achievement.setCategoryId(
					HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(achiHex, 14, 16)));
			achievement.setMetricId(
					HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(achiHex, 18, 20)));
			achievement.setMetricTargetInt(
					HEXUtils.byteArrayToInt(Arrays.copyOfRange(achiHex, 22, 26)));
			achievement.setImageName(
					getCDATStringByShortCHDRId(achiHex, 26, 28, sbinJson.getCDATStrings()));
			achievement.setImageText(
					getCDATStringByShortCHDRId(achiHex, 28, 30, sbinJson.getCDATStrings()));
			
			List<Integer> metricMilestones = new ArrayList<>();
			List<byte[]> metricMilestonesEntries = readDATABlockObjectMap(
					dataBlock.getBlockElements().get(dataIndex + 1));
			for (byte[] milestoneDataId : metricMilestonesEntries) {
				byte[] milestoneHexData = dataBlock.getBlockElements().get(HEXUtils.byteArrayToInt(milestoneDataId));
				metricMilestones.add(HEXUtils.byteArrayToInt(Arrays.copyOfRange(milestoneHexData, 2, 6)));
			}
			achievement.setMetricMilestones(metricMilestones);
			achievementsArray.add(achievement);
		}
		sbinJson.setAchievementArray(achievementsArray);
		
		sbinJson.setDataElements(sbinJson.getDataElements().subList(0, 8));
		// Keep only structure-related strings in CDAT output
		sbinJson.setCDATStrings(sbinJson.getCDATStrings().subList(0, firstStringCHDRId));
	}
	
	private void unpackPlaylistsData(SBinJson sbinJson, SBinBlockObj dataBlock) {
		List<byte[]> playlistsMap = readDATABlockObjectMap(dataBlock.getBlockElements().get(1));
		List<SBinPlaylistObj> playlistsJson = new ArrayList<>();
		
		for (byte[] playlistDescPos : playlistsMap) {
			int dataIndex = HEXUtils.byteArrayToInt(playlistDescPos);
			byte[] playlistDescHex = dataBlock.getBlockElements().get(dataIndex);
			
			SBinPlaylistObj playlist = new SBinPlaylistObj();
			playlist.setOhdrDescRemainder(sbinJson.getDataElements().get(dataIndex).getOhdrUnkRemainder());
			playlist.setName(
					getCDATStringByShortCHDRId(playlistDescHex, 12, 14, sbinJson.getCDATStrings()));
			
			List<byte[]> tracks = readDATABlockObjectMap(
					dataBlock.getBlockElements().get(dataIndex + 1));
			playlist.setOhdrStruRemainder(sbinJson.getDataElements().get(dataIndex + 1).getOhdrUnkRemainder());
			for (byte[] trackId : tracks) {
				int trackIndex = HEXUtils.byteArrayToInt(trackId);
				byte[] trackHex = dataBlock.getBlockElements().get(trackIndex);
				SBinPlaylistTrackObj trackObj = new SBinPlaylistTrackObj();
				
				trackObj.setOhdrUnkRemainder(sbinJson.getDataElements().get(trackIndex).getOhdrUnkRemainder());
				trackObj.setFilePath(
						getCDATStringByShortCHDRId(trackHex, 12, 14, sbinJson.getCDATStrings()));
				trackObj.setArtist(
						getCDATStringByShortCHDRId(trackHex, 22, 24, sbinJson.getCDATStrings()));
				trackObj.setTitle(
						getCDATStringByShortCHDRId(trackHex, 32, 34, sbinJson.getCDATStrings()));
				playlist.addToPlaylist(trackObj);
			}
			playlistsJson.add(playlist);
		}
		sbinJson.setPlaylistsArray(playlistsJson);
		
		sbinJson.setDataElements(sbinJson.getDataElements().subList(0, 1));
		// Keep only structure-related strings in CDAT output. 
		// This time string ordering is unusual, so we proceed with hard-coded position
		sbinJson.setCDATStrings(sbinJson.getCDATStrings().subList(0, 13));
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
			chdrHexStream.write(HEXUtils.intToByteArrayLE(chdrToCdatPos, 0x4));
			chdrHexStream.write(HEXUtils.intToByteArrayLE(chdrToCdatSize, 0x4));
		}
		block.setBlockBytes(chdrHexStream.toByteArray());
		
		setSBinBlockAttributes(block);
		return block;
	}
	
	private SBinBlockObj createOHDRBlock(SBinJson sbinJson, SBinBlockObj dataBlock, byte[] header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		
		ByteArrayOutputStream ohdrHexStream = new ByteArrayOutputStream();
		ohdrHexStream.write(HEXUtils.intToByteArrayLE(0x1, 0x4)); // First element in OHDR
		int ohdrByteLength = 0x0;
		for (int i = 0; i < dataBlock.getOHDRMapTemplate().size() - 1; i++) {
			SBinOHDREntry entry = dataBlock.getOHDRMapTemplate().get(i);
			int entryLength = entry.getValue();
			// I don't know why some of OHDR entries have a small remainder in values.
			// Adding it as it is works well, usually
			ohdrByteLength += entryLength;
			ohdrHexStream.write(HEXUtils.intToByteArrayLE(ohdrByteLength + entry.getRemainder(), 0x4));
		} // Ignore last DATA element - last OHDR entry ends on DATA length
		
		if (sbinJson.getSBinType() == SBinType.CAREER) {
			prepareCareerOHDRForSBinBlock(ohdrHexStream, sbinJson.getCareerGarageCarsArray().size());
		}
		block.setBlockBytes(ohdrHexStream.toByteArray());
		
		setSBinBlockAttributes(block);
		return block;
	}
	
	private SBinBlockObj createDATABlock(SBinJson sbinJson, byte[] header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		
		ByteArrayOutputStream dataHexStream = new ByteArrayOutputStream();
		for (SBinDataElement dataEntry : sbinJson.getDataElements()) {
			byte[] dataEntryHex = HEXUtils.decodeHexStr(dataEntry.getHexValue());
			dataHexStream.write(dataEntryHex);
			block.addToOHDRMapTemplate(dataEntryHex.length, dataEntry.getOhdrUnkRemainder());
		} // Save common or unknown DATA info
		
		switch(sbinJson.getSBinType()) {
		case STRINGDATA:
			prepareStringDataForSBinBlock(dataHexStream, sbinJson.getStrDataEntriesArray());
			break;
		case CAREER:
			prepareCareerDataForSBinBlock(dataHexStream, sbinJson);
			break;
		case ACHIEVEMENTS:
			dataHexStream.write(prepareAchievementDataForSBinBlock(block, sbinJson));
			break;
		case PLAYLISTS:
			dataHexStream.write(preparePlaylistsDataForSBinBlock(block, sbinJson));
			break;
		default: break;
		}
		block.setBlockBytes(dataHexStream.toByteArray());
		
		setSBinBlockAttributes(block);
		return block;
	}
	
	// Very primitive
	private SBinBlockObj createBARGBlock(SBinJson sbinJson, byte[] header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(header);
		
		byte[] imageHex = Files.readAllBytes(Paths.get(sbinJson.getFileName() + ".dds"));
		block.setBlockBytes(Arrays.copyOfRange(imageHex, DDS_HEADER_SIZE, imageHex.length));
		
		setSBinBlockAttributes(block);
		return block;
	}
	
	private void prepareStringDataForSBinBlock(ByteArrayOutputStream dataHexStream, List<SBinStringDataEntry> entries) throws IOException {
		ByteArrayOutputStream strDataHexStream = new ByteArrayOutputStream();
		int dataStrCount = 0;
		
		for (SBinStringDataEntry entry : entries) {
			if (entry.isTextEntry()) {continue;}
			dataStrCount++;
			strDataHexStream.write(HEXUtils.shortToBytes((short)entry.getCHDRId().intValue()));
			strDataHexStream.write(HEXUtils.shortToBytes((short)entry.getCHDRIdTextRef().intValue()));
			strDataHexStream.write(HEXUtils.decodeHexStr(entry.getHalVersionValue()));
		}
		dataHexStream.write(HEXUtils.intToByteArrayLE(dataStrCount, 0x4)); // StringData entries count, without Text entries
		dataHexStream.write(strDataHexStream.toByteArray());
	}
	
	private void prepareCareerOHDRForSBinBlock(ByteArrayOutputStream ohdrHexStream, int careerGarageCarsCount) throws IOException {
		for (int i = 0; i < careerGarageCarsCount; i++) {
			ohdrHexStream.write(HEXUtils.intToByteArrayLE(
					CAREER_OHDR_GARAGECARS_BASEVALUE + (0x20 * careerGarageCarsCount) + (0x20 * i), 0x4));
		}
		
	}
	
	private void prepareCareerDataForSBinBlock(ByteArrayOutputStream dataHexStream, SBinJson sbinJson) throws IOException {
		ByteArrayOutputStream careerData1HexStream = new ByteArrayOutputStream();
		ByteArrayOutputStream careerData2HexStream = new ByteArrayOutputStream();
		int careerCarsGarageCount = sbinJson.getCareerGarageCarsArray().size();
		careerData1HexStream.write(HEXUtils.intToByteArrayLE(careerCarsGarageCount, 0x4));
		// First value in part 2 block
		careerData2HexStream.write(SHORTBYTE_EMPTY); 
		careerData2HexStream.write(CAREER_DATA_P2_SHORTBYTES);
		
		int minimalCareerDataP1Value = HEXUtils.byteArrayToInt(HEXUtils.decodeHexStr(sbinJson.getCareerFirstDATAByteValue()));
		int minimalCareerDataP2Value = sbinJson.getCDATStrings().size();
		
		for (int i = 0; i < sbinJson.getCareerGarageCarsArray().size(); i++) {
			careerData1HexStream.write(HEXUtils.intToByteArrayLE(minimalCareerDataP1Value + i, 0x4));
			careerData2HexStream.write(HEXUtils.shortToBytes(minimalCareerDataP2Value + i));
			if (i + 1 < sbinJson.getCareerGarageCarsArray().size()) {
				careerData2HexStream.write(CAREER_DATA_P2_SHORTBYTES);
			} else { // Last one must be empty
				careerData2HexStream.write(SHORTBYTE_EMPTY);
			}
		}
		dataHexStream.write(careerData1HexStream.toByteArray());
		dataHexStream.write(careerData2HexStream.toByteArray());
	}
	
	private byte[] prepareAchievementDataForSBinBlock(SBinBlockObj block, SBinJson sbinJson) throws IOException {
		ByteArrayOutputStream achievementDataHexStream = new ByteArrayOutputStream();
		int orderId = sbinJson.getDataElements().size();
		
		orderId++; // Skip map entry
		SBinStructureEntryHex achiMap = new SBinStructureEntryHex();
		achiMap.setHeader(HEXUtils.intToByteArrayLE(0xF, 0x4));
		achiMap.setSize(HEXUtils.intToByteArrayLE(sbinJson.getAchievementArray().size(), 0x4));
		
		ByteArrayOutputStream achiCollectionHexStream = new ByteArrayOutputStream();
		List<SBinOHDREntry> ohdrMapTemplate = new ArrayList<>();
		for (SBinAchievementEntry achievement : sbinJson.getAchievementArray()) {
			achiMap.addToDataIds(HEXUtils.intToByteArrayLE(orderId, 0x4)); // Add achievement to map
			orderId++;
			
			SBinAchievementEntryHex achiHex = new SBinAchievementEntryHex();
			achiHex.setOhdrUnkRemainder(achievement.getOhdrUnkRemainder());
			achiHex.setName(processStringInCDAT(sbinJson.getCDATStrings(), achievement.getName()));
			achiHex.setDesc(processStringInCDAT(sbinJson.getCDATStrings(), achievement.getDesc()));
			achiHex.setPoints(HEXUtils.intToByteArrayLE(achievement.getPointsInt(), 0x4));
			achiHex.setAutologAwardId(processStringInCDAT(sbinJson.getCDATStrings(), achievement.getAutologAwardId()));
			achiHex.setCategoryId(HEXUtils.intToByteArrayLE(achievement.getCategoryId(), 0x4));
			achiHex.setMetricId(HEXUtils.intToByteArrayLE(achievement.getMetricId(), 0x4));
			achiHex.setMetricTarget(HEXUtils.intToByteArrayLE(achievement.getMetricTargetInt(), 0x4));
			achiHex.setImageName(processStringInCDAT(sbinJson.getCDATStrings(), achievement.getImageName()));
			achiHex.setImageText(processStringInCDAT(sbinJson.getCDATStrings(), achievement.getImageText()));
			achiHex.setOrderId(HEXUtils.shortToBytes((short)orderId));
			
			SBinStructureEntryHex structure = new SBinStructureEntryHex();
			structure.setHeader(HEXUtils.intToByteArrayLE(0xF, 0x4));
			structure.setSize(HEXUtils.intToByteArrayLE(achievement.getMetricMilestones().size(), 0x4));
			
			for (int i = 0; i < achievement.getMetricMilestones().size(); i++) {
				structure.addToDataIds(HEXUtils.intToByteArrayLE(orderId + i + 1, 0x4));
			} // MetricMilestones map
			achiHex.setMetricMilestonesMap(structure);
			orderId++;
			
			for (Integer milestone : achievement.getMetricMilestones()) {
				SBinAchievementMilestoneEntryHex milestoneEntry = new SBinAchievementMilestoneEntryHex();
				milestoneEntry.setHeader(HEXUtils.shortToBytes((short)0x4));
				milestoneEntry.setIntValue(HEXUtils.intToByteArrayLE(milestone, 0x4));
				achiHex.addToMetricMilestones(milestoneEntry);
				orderId++;
			}
			achiCollectionHexStream.write(achiHex.toByteArray());
			ohdrMapTemplate.addAll(achiHex.ohdrMapTemplate());
		}
		// Achievement map comes before the achievement objects
		achievementDataHexStream.write(achiMap.toByteArray());
		achievementDataHexStream.write(achiCollectionHexStream.toByteArray());
		
		block.addToOHDRMapTemplate(achiMap.getByteSize(), 0);
		block.getOHDRMapTemplate().addAll(ohdrMapTemplate);
		subLast2BytesOHDR(block);
		
		byte[] finalAchievementsBytes = achievementDataHexStream.toByteArray();
		// Cut the last 2 bytes like in original files, supposed to be empty padding
		return Arrays.copyOfRange(finalAchievementsBytes, 0, finalAchievementsBytes.length - 2);
	}
	
	private byte[] preparePlaylistsDataForSBinBlock(SBinBlockObj block, SBinJson sbinJson) throws IOException {
		ByteArrayOutputStream playlistsDataHexStream = new ByteArrayOutputStream();
		int orderId = sbinJson.getDataElements().size();
		
		orderId++; // Skip map entry
		SBinStructureEntryHex playlistsMap = new SBinStructureEntryHex();
		playlistsMap.setHeader(HEXUtils.intToByteArrayLE(0xF, 0x4));
		playlistsMap.setSize(HEXUtils.intToByteArrayLE(sbinJson.getPlaylistsArray().size(), 0x4));
		playlistsMap.setPadding(new byte[0]);
		
		ByteArrayOutputStream playlistCollectionHexStream = new ByteArrayOutputStream();
		List<SBinOHDREntry> ohdrMapTemplate = new ArrayList<>();
		for (SBinPlaylistObj playlist : sbinJson.getPlaylistsArray()) {
			playlistsMap.addToDataIds(HEXUtils.intToByteArrayLE(orderId, 0x4)); // Add playlist to map
			orderId++;
			
			SBinPlaylistEntryHex playlistHex = new SBinPlaylistEntryHex();
			playlistHex.setOhdrDescRemainder(playlist.getOhdrDescRemainder());
			playlistHex.setOhdrStruRemainder(playlist.getOhdrStruRemainder());
			playlistHex.setHeader(HEXUtils.shortToBytes((short)0x2));
			playlistHex.setUnkHex1(PLAYLISTS_DATA_DESC_UNK1);
			playlistHex.setName(processStringInCDAT(sbinJson.getCDATStrings(), playlist.getName()));
			playlistHex.setUnkHex2(PLAYLISTS_DATA_DESC_UNK2);
			playlistHex.setOrderId(HEXUtils.shortToBytes((short)orderId));
			
			SBinStructureEntryHex tracksMap = new SBinStructureEntryHex();
			tracksMap.setHeader(HEXUtils.intToByteArrayLE(0xF, 0x4));
			tracksMap.setSize(HEXUtils.intToByteArrayLE(playlist.getPlaylist().size(), 0x4));
			tracksMap.setPadding(new byte[0]);
			
			for (int i = 0; i < playlist.getPlaylist().size(); i++) {
				tracksMap.addToDataIds(HEXUtils.intToByteArrayLE(orderId + i + 1, 0x4));
			} 
			playlistHex.setTracksMap(tracksMap);
			orderId++;
			
			for (SBinPlaylistTrackObj track : playlist.getPlaylist()) {
				SBinPlaylistTrackHex trackEntry = new SBinPlaylistTrackHex();
				trackEntry.setOhdrUnkRemainder(track.getOhdrUnkRemainder());
				trackEntry.setHeader(HEXUtils.shortToBytes((short)0x3));
				trackEntry.setUnkHex1(PLAYLISTS_DATA_TRACK_UNK1);
				trackEntry.setFilePath(processStringInCDAT(sbinJson.getCDATStrings(), track.getFilePath()));
				trackEntry.setUnkHex2(PLAYLISTS_DATA_TRACK_UNK2);
				trackEntry.setArtist(processStringInCDAT(sbinJson.getCDATStrings(), track.getArtist()));
				trackEntry.setUnkHex3(PLAYLISTS_DATA_TRACK_UNK3);
				trackEntry.setTitle(processStringInCDAT(sbinJson.getCDATStrings(), track.getTitle()));
				playlistHex.addToPlaylistTracks(trackEntry);
				orderId++;
			}
			
			playlistCollectionHexStream.write(playlistHex.toByteArray());
			ohdrMapTemplate.addAll(playlistHex.ohdrMapTemplate());
		}
		// Playlists map comes before the Playlists objects
		playlistsDataHexStream.write(playlistsMap.toByteArray());
		playlistsDataHexStream.write(playlistCollectionHexStream.toByteArray());

		block.addToOHDRMapTemplate(playlistsMap.getByteSize(), 1);
		block.getOHDRMapTemplate().addAll(ohdrMapTemplate);
		return playlistsDataHexStream.toByteArray();
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
	
	private byte[] getBytesFromCurPos(byte[] data, int to) {
		return Arrays.copyOfRange(data, curPos, curPos + to);
	}
	
}
