import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
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
import util.DataUtils;
import util.HEXClasses.*;
import util.HEXUtils;
import util.LaunchParameters;
import util.SBinBlockType;
import util.SBinEnumUtils;
import util.SBinFieldType;
import util.SBinMapUtils;
import util.SBinMapUtils.SBinMapType;
import util.TextureUtils;

public class SBin {
	
	Gson gson = new Gson();
	
	private static final byte[] SHORTBYTE_EMPTY = new byte[2];
	//
	private static final byte[] PLAYLISTS_DATA_DESC_UNK1 = HEXUtils.decodeHexStr("1C0002000D000C000000");
	private static final byte[] PLAYLISTS_DATA_DESC_UNK2 = HEXUtils.decodeHexStr("04000F00180000000000");
	private static final byte[] PLAYLISTS_DATA_TRACK_UNK1 = HEXUtils.decodeHexStr("220005000D000C000000");
	private static final byte[] PLAYLISTS_DATA_TRACK_UNK2 = HEXUtils.decodeHexStr("07000D0016000000");
	private static final byte[] PLAYLISTS_DATA_TRACK_UNK3 = HEXUtils.decodeHexStr("08000D0020000000");
	//
	private static final String FIRST_DATA_STRUCTNAME = "FirstDATAElement";
	private static final String HEX_DATA_TYPE = "HEXData";
	
	private static int curPos = 0x0;
	
	//
	
	public static void startup() {
		SBinMapUtils.initMapTypes();
	}

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

		// ENUM: Enum objects stored as a DATA block maps
		SBinBlockObj enumBlock = processSBinBlock(sbinData, SBinBlockType.ENUM, SBinBlockType.STRU);		
		// STRU: object structures of DATA block
		SBinBlockObj struBlock = processSBinBlock(sbinData, SBinBlockType.STRU, SBinBlockType.FIEL);
		// FIEL: info fields for Structs
		SBinBlockObj fielBlock = processSBinBlock(sbinData, SBinBlockType.FIEL, SBinBlockType.OHDR);	
		// OHDR: map of DATA block
		SBinBlockObj ohdrBlock = processSBinBlock(sbinData, SBinBlockType.OHDR, SBinBlockType.DATA);		
		// DATA: various objects info
		SBinBlockObj dataBlock = processSBinBlock(sbinData, SBinBlockType.DATA, SBinBlockType.CHDR);	
		// CHDR: map of CDAT block
		SBinBlockObj chdrBlock = processSBinBlock(sbinData, SBinBlockType.CHDR, SBinBlockType.CDAT);
		// CDAT: field names & string variables
		SBinBlockObj cdatBlock = processSBinBlock(sbinData, SBinBlockType.CDAT, 
				sbinJson.getSBinType() == SBinType.TEXTURE ? SBinBlockType.BULK : null);
		sbinJson.setCDATStrings(prepareCDATStrings(chdrBlock.getBlockElements(), cdatBlock.getBlockBytes()));
		
		if (LaunchParameters.isDATAObjectsUnpackDisabled()) {
			sbinJson.setENUMHexStr(HEXUtils.hexToString(enumBlock.getBlockBytes()).toUpperCase());
			sbinJson.setSTRUHexStr(HEXUtils.hexToString(struBlock.getBlockBytes()).toUpperCase());
			sbinJson.setFIELHexStr(HEXUtils.hexToString(fielBlock.getBlockBytes()).toUpperCase());
		} else {
			readEnumHeaders(sbinJson, enumBlock);
			readStructsAndFields(sbinJson, struBlock, fielBlock);
		}
		parseDATABlock(sbinJson, ohdrBlock, dataBlock);
		updateEnumRelatedObjects(sbinJson);
		// Used for separate file editors, not all of .sb files gets proper objects layouts
		switch(sbinJson.getSBinType()) {
		case PLAYLISTS:
			unpackPlaylistsData(sbinJson, dataBlock);
			break;
		case TEXTURE:
			// BULK: Image mipmap offsets
			SBinBlockObj bulkBlock = processSBinBlock(sbinData, SBinBlockType.BULK, SBinBlockType.BARG);
			// BARG: Image plain data
			SBinBlockObj bargBlock = processSBinBlock(sbinData, SBinBlockType.BARG, null);
			TextureUtils.extractImage(sbinJson, bulkBlock, bargBlock.getBlockBytes());
			break;
		default: break;
		}
		clearJsonOutputStuff(sbinJson);
		
		gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonOut = gson.toJson(sbinJson);
		Files.write(Paths.get(sbinJson.getFileName() + ".json"), jsonOut.getBytes(StandardCharsets.UTF_8));
	}

	public void repackSBin(String filePath) throws IOException {
		Reader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
		SBinJson sbinJsonObj = new Gson().fromJson(reader, new TypeToken<SBinJson>(){}.getType());
		reader.close();
		sbinJsonObj.setSBinType(sbinJsonObj.getSBinType());

		// ENUM
		SBinBlockObj enumBlock = createSBinBlock(sbinJsonObj, SBinBlockType.ENUM);
		byte[] finalENUM = buildSBinBlock(enumBlock);
		// STRU & FIEL
		SBinBlockObj struBlock = new SBinBlockObj();
		SBinBlockObj fielBlock = new SBinBlockObj();
		createSTRUFIELBlocks(sbinJsonObj, struBlock, fielBlock);
		byte[] finalFIEL = buildSBinBlock(fielBlock);
		byte[] finalSTRU = buildSBinBlock(struBlock);
		// DATA & OHDR
		SBinBlockObj dataBlock = createSBinBlock(sbinJsonObj, SBinBlockType.DATA);
		byte[] finalDATA = buildSBinBlock(dataBlock);
		//
		SBinBlockObj ohdrBlock = createOHDRBlock(sbinJsonObj, dataBlock, SBinBlockType.OHDR);
		byte[] finalOHDR = buildSBinBlock(ohdrBlock);
		// CHDR & CDAT
		SBinBlockObj cdatBlock = createSBinBlock(sbinJsonObj, SBinBlockType.CDAT);
		byte[] finalCDAT = buildSBinBlock(cdatBlock);
		//
		SBinBlockObj chdrBlock = createCHDRBlock(cdatBlock.getBlockElements(), SBinBlockType.CHDR);
		byte[] finalCHDR = buildSBinBlock(chdrBlock);
		
		ByteArrayOutputStream additionalBlocksStream = new ByteArrayOutputStream();
		if (sbinJsonObj.getSBinType() == SBinType.TEXTURE) {
			// BULK & BARG
			createBULKBARGBlocks(sbinJsonObj, additionalBlocksStream);
		} 
		ByteArrayOutputStream fileOutputStr = new ByteArrayOutputStream();
		fileOutputStr.write(SBinBlockType.getBytes(SBinBlockType.SBIN));
		fileOutputStr.write(HEXUtils.intToByteArrayLE(0x3, 0x4)); // Version 3
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

	public void getFNVHash(String filePath) throws IOException {
		byte[] fileArray = Files.readAllBytes(Paths.get(filePath));
		byte[] filePart;

		filePart = Arrays.copyOfRange(fileArray, 0, fileArray.length);

		int hexHash = FNV1.hash32(filePart);
		String output = Integer.toHexString(HEXUtils.hexRev(hexHash)).toUpperCase();
		String length = Integer.toHexString(HEXUtils.hexRev(filePart.length)).toUpperCase();
		System.out.println("FNV1 Hash: " + output);
		System.out.println("Block Length: " + length);
	}

	//
	// SBin block methods
	//

	// TODO Must read it by 4-bytes-block method. 
	// Additional empty bytes after blocks exists to help fit the info, for 4-bytes-block reading method
	private SBinBlockObj processSBinBlock(byte[] sbinData, SBinBlockType header, SBinBlockType nextHeader) {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(SBinBlockType.getBytes(header));
		changeCurPos(0x4); // Skip header
		block.setBlockSize(getBytesFromCurPos(sbinData, 0x4));
		block.setBlockSizeInt(HEXUtils.byteArrayToInt(block.getBlockSize()));
		changeCurPos(0x8); // Skip size + hash
		if (block.getBlockSizeInt() != 0x0) {
			block.setBlockBytes(getBytesFromCurPos(sbinData, block.getBlockSizeInt()));
			switch(header) {
			case OHDR:
				block.setBlockElements(HEXUtils.splitByteArray(block.getBlockBytes(), 0x4));
				break;
			case STRU:
				block.setBlockElements(HEXUtils.splitByteArray(block.getBlockBytes(), 0x6));
				break;
			case ENUM: case FIEL: case CHDR: case BULK:
				block.setBlockElements(HEXUtils.splitByteArray(block.getBlockBytes(), 0x8));
				break;
			default: break;
			}
			changeCurPos(block.getBlockSizeInt()); 
		}

		byte[] fielHeaderCheck = getBytesFromCurPos(sbinData, 0x4);
		while (nextHeader != null && !Arrays.equals(fielHeaderCheck, SBinBlockType.getBytes(nextHeader))) {
			changeCurPos(0x1); // Happens in some files and usually on STRU block, with additional empty 0x2 over the block size
			block.setBlockEmptyBytesCount(block.getBlockEmptyBytesCount() + 1);
			fielHeaderCheck = getBytesFromCurPos(sbinData, 0x4);
		}
		
		return block;
	}

	private SBinBlockObj createSBinBlock(SBinJson sbinJson, SBinBlockType header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(SBinBlockType.getBytes(header));
		
		switch(header) {
		case ENUM:
			block.setBlockBytes(createENUMBlockBytes(sbinJson));
			break;
		case OHDR:
			break;
		case DATA:
			createDATABlockBytes(sbinJson, block);
			break;
		case CHDR:
			break;
		case CDAT:
			createCDATBlock(sbinJson, block);
			if (!sbinJson.getSBinType().equals(SBinType.TEXTURE)) {
				block.setLastBlock(true);
			}
			break;
		case BULK:
			break;
		case BARG:
			break;
		default: 
			block.setBlockBytes(new byte[0]);
			break;
		}
		
		setSBinBlockAttributes(block);
		return block;
	}
	
	private byte[] createENUMBlockBytes(SBinJson sbinJson) throws IOException {
		ByteArrayOutputStream enumHexStream = new ByteArrayOutputStream();
		if (sbinJson.getENUMHexStr() != null) {
			enumHexStream.write(HEXUtils.decodeHexStr(sbinJson.getENUMHexStr()));
		}
		else for (SBinEnum enumEntry : sbinJson.getEnums()) {
			enumHexStream.write(DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), enumEntry.getName()));
			enumHexStream.write(SHORTBYTE_EMPTY);
			enumHexStream.write(HEXUtils.decodeHexStr(enumEntry.getDataIdMapRef()));
			enumHexStream.write(SHORTBYTE_EMPTY);
		} 
		return enumHexStream.toByteArray();
	}
	
	private void createSTRUFIELBlocks(SBinJson sbinJson, SBinBlockObj struBlock, SBinBlockObj fielBlock) throws IOException {
		struBlock.setHeader(SBinBlockType.getBytes(SBinBlockType.STRU));
		fielBlock.setHeader(SBinBlockType.getBytes(SBinBlockType.FIEL));
		
		if (sbinJson.getSTRUHexStr() != null) {
			struBlock.setBlockBytes(HEXUtils.decodeHexStr(sbinJson.getSTRUHexStr()));
			fielBlock.setBlockBytes(HEXUtils.decodeHexStr(sbinJson.getFIELHexStr()));
		} else {
			ByteArrayOutputStream struHexStream = new ByteArrayOutputStream();
			ByteArrayOutputStream fielHexStream = new ByteArrayOutputStream();
			int fieldId = 0;
			// Place "empty" fields first
			for (SBinField emptyFld : sbinJson.getEmptyFields()) {
				fielHexStream.write(HEXUtils.decodeHexStr(emptyFld.getHexValue()));
				fieldId++;
			}
			
			String nextFieldName = "";
			for (SBinStruct struct : sbinJson.getStructs()) {
				struHexStream.write(DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), struct.getName()));
				boolean isFirstFieldPassed = false;
				if (sbinJson.getStructs().indexOf(struct) + 1 < sbinJson.getStructs().size()) {
					SBinStruct nextStruct = sbinJson.getStructs().get(sbinJson.getStructs().indexOf(struct) + 1);
					if (struct.getFieldsArray().size() == 1) {
						nextFieldName = nextStruct.getFieldsArray().get(0).getName();
					}
				}
				
				int fieldCount = 0;
				for (SBinField field : struct.getFieldsArray()) {
					if (!isFirstFieldPassed) {
						struHexStream.write(HEXUtils.shortToBytes((short)fieldId));
						isFirstFieldPassed = true;
					}
					if (nextFieldName.contentEquals(field.getName())) {
						System.out.println("same: " + field.getName());
						nextFieldName = "";
						continue;
					}
					System.out.println("added: " + field.getName());
					fielHexStream.write(DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), field.getName()));
					fielHexStream.write(HEXUtils.shortToBytes(
							(short)SBinEnumUtils.getIdByStringName(field.getType())));
					fielHexStream.write(HEXUtils.shortToBytes((short)field.getStartOffset()));
					fielHexStream.write(HEXUtils.shortToBytes((short)field.getSpecOrderId()));
					fieldId++;
					fieldCount++;
				}
				byte[] countToNextStruct = HEXUtils.shortToBytes((short)fieldCount);
				struHexStream.write(countToNextStruct);
				nextFieldName = "";
			}
			struBlock.setBlockBytes(struHexStream.toByteArray());
			fielBlock.setBlockBytes(fielHexStream.toByteArray());
		} 
		
		setSBinBlockAttributes(struBlock);
		setSBinBlockAttributes(fielBlock);
	}

	private byte[] buildSBinBlock(SBinBlockObj block) throws IOException {
		ByteArrayOutputStream enumStream = new ByteArrayOutputStream();
		enumStream.write(block.getHeader());
		enumStream.write(block.getBlockSize());
		enumStream.write(block.getFnv1Hash());
		enumStream.write(block.getBlockBytes());
		int remainder = enumStream.size() % 4;
		if (!block.isLastBlock() && remainder != 0) {
			enumStream.write(new byte[4 - remainder]);
		}
		return enumStream.toByteArray();
	}
	
	private void createBULKBARGBlocks(SBinJson sbinJson, ByteArrayOutputStream additionalBlocksStream) throws IOException {
		SBinBlockObj bargBlock = new SBinBlockObj();
		bargBlock.setHeader(SBinBlockType.getBytes(SBinBlockType.BARG));
		TextureUtils.repackImage(bargBlock, sbinJson);
		setSBinBlockAttributes(bargBlock);
		bargBlock.setLastBlock(true);
		byte[] finalBARG = buildSBinBlock(bargBlock);
		
		SBinBlockObj bulkBlock = new SBinBlockObj();
		bulkBlock.setHeader(SBinBlockType.getBytes(SBinBlockType.BULK));
		bulkBlock.setBlockBytes(bargBlock.getBULKMap());
		setSBinBlockAttributes(bulkBlock);
		byte[] finalBULK = buildSBinBlock(bulkBlock);
		
		additionalBlocksStream.write(finalBULK);
		additionalBlocksStream.write(finalBARG);
	}
	
	private void setSBinBlockAttributes(SBinBlockObj block) {
		block.setFnv1Hash(HEXUtils.intToByteArrayLE(FNV1.hash32(block.getBlockBytes()), 0x4)); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(HEXUtils.intToByteArrayLE(block.getBlockBytes().length, 0x4));
		}
	}
	
	// Some DATA entries ends with short "00 00" instead of 4 bytes
	private List<byte[]> readDATABlockObjectMap(byte[] map, int entrySize) {
		List<byte[]> entries = new ArrayList<>();
		int entriesCount = HEXUtils.byteArrayToInt(Arrays.copyOfRange(
				map, SBinMapUtils.HEADERENTRY_SIZE, SBinMapUtils.HEADERFULL_SIZE));
		if (entriesCount != 0) {
			byte[] mapEntries = Arrays.copyOfRange(map, SBinMapUtils.HEADERFULL_SIZE, map.length);
			
			for (int i = 0; i < entriesCount; i++) {
				int offset = entrySize * i;
				entries.add(Arrays.copyOfRange(mapEntries, offset, entrySize + offset));
			}
		}
		return entries;
	}
	
	private String getCDATStringByShortCHDRId(byte[] bytes, int startIndex, int endIndex, List<SBinCDATEntry> cdatStrings) {
		int hexCHDRId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(bytes, startIndex, endIndex));
		return cdatStrings.get(hexCHDRId).getString();
	}
	
	private SBinCDATEntry getCDATEntryByShortCHDRId(byte[] bytes, int startIndex, int endIndex, List<SBinCDATEntry> cdatStrings) {
		int hexCHDRId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(bytes, startIndex, endIndex));
		return cdatStrings.get(hexCHDRId);
	}
	
	//
	// SBin object-related functions
	//
	
	private void readStructsAndFields(SBinJson sbinJson, SBinBlockObj struBlock, SBinBlockObj fielBlock) {
		if (struBlock.getBlockSizeInt() == 0 || fielBlock.getBlockSizeInt() == 0) {return;}
		
		int firstReadableField = 0;
		int id = 0;
		for (byte[] structBytes : struBlock.getBlockElements()) {
			SBinStruct struct = new SBinStruct();
			struct.setId(id);
			struct.setName(
					getCDATStringByShortCHDRId(structBytes, 0, 2, sbinJson.getCDATStrings()));
			
			int firstFieldId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(structBytes, 2, 4));
			int countToNextStruct = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(structBytes, 4, 6));
			if (countToNextStruct == 0) {
				countToNextStruct = 1;
			}
			if (firstReadableField == 0 && firstFieldId > 0) {
				firstReadableField = firstFieldId; // Read these unknown fields later
			}
			for (int i = 0; i < countToNextStruct; i++) {
				struct.addToFields(readField(
						sbinJson, fielBlock, i, firstFieldId, countToNextStruct));
			}
			//System.out.println("id: " + struct.getId() + ", name: " + struct.getName() + ", countToNextStruct: " + countToNextStruct + ", size real: " + struct.getFieldsArray().size());
			struct.getFieldsArray().get(struct.getFieldsArray().size() - 1).setDynamicSize(true);
			sbinJson.addStruct(struct);
			id++;
		}
		// Do it after the initial iteration, since any other sub-struct could be mentioned
		for (SBinStruct struct : sbinJson.getStructs()) {
			for (SBinField field : struct.getFieldsArray()) {
				if (field.getFieldTypeEnum() != null && 
						field.getFieldTypeEnum().equals(SBinFieldType.SUB_STRUCT)) {
					field.setSubStruct(sbinJson.getStructs().get(field.getSpecOrderId()).getName());
				}
			}
		}
		if (firstReadableField != 0) {
			for (int i = 0; i < firstReadableField; i++) {
				sbinJson.addEmptyField(readField(sbinJson, fielBlock, i, 0, 0));
			}
		}
 	}
	
	private SBinField readField(SBinJson sbinJson, SBinBlockObj fielBlock, 
			int i, int firstFieldId, int fieldsCount) {
		byte[] fieldHex = fielBlock.getBlockElements().get(firstFieldId + i);
		SBinField fieldObj = new SBinField();
		
		int nameCHDRId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(fieldHex, 0, 2));
		fieldObj.setName(sbinJson.getCDATStrings().get(nameCHDRId).getString());
		fieldObj.setStartOffset(HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(fieldHex, 4, 6)));
		if (nameCHDRId == 0) {
			fieldObj.setHexValue(HEXUtils.hexToString(fielBlock.getBlockElements().get(i)));
		}
		
		// this order ID could be used for Enum IDs, and in Maps for some reason
		fieldObj.setSpecOrderId(HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(fieldHex, 6, 8)));
		int type = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(fieldHex, 2, 4));
		
		fieldObj.setFieldTypeEnum(SBinFieldType.valueOf(type));
		String fieldEnumStr;
		if (fieldObj.getFieldTypeEnum() == null) {
			fieldEnumStr = "UNK_0x" + Integer.toHexString(type);
			// Do not parse DATA here - unknown entry size
		} else {
			fieldEnumStr = fieldObj.getFieldTypeEnum().toString();
		}
		fieldObj.setFieldSize(getFieldSize(i, fieldsCount, fieldObj.getStartOffset(), fielBlock, firstFieldId));
		fieldObj.setType(fieldEnumStr);
		
		if (fieldObj.getFieldTypeEnum() != null 
				&& fieldObj.getFieldTypeEnum().equals(SBinFieldType.ENUM_ID_INT32)) {
			fieldObj.setEnumJsonPreview(sbinJson.getEnums().get(fieldObj.getSpecOrderId()).getName());
		}
		return fieldObj;
	}
	
	// Fields can have a different sizes even for the same types, also there is no way to know about the size of last element
	// And DATA elements have varied padding bytes sometimes
	private int getFieldSize(int i, int fieldsCount, int startOffset, SBinBlockObj fielBlock, int firstFieldId) {
		int nextFieldStartOffset = startOffset;
		if (i + 1 < fieldsCount) {
			nextFieldStartOffset = HEXUtils.twoLEByteArrayToInt(
					Arrays.copyOfRange(fielBlock.getBlockElements().get(firstFieldId + i + 1), 4, 6));
		}
		return nextFieldStartOffset - startOffset;
	}
	
	private void parseDATABlock(SBinJson sbinJson, SBinBlockObj ohdrBlock, SBinBlockObj dataBlock) {
		ohdrBlock.getBlockElements().remove(0); // First one is always 0x1
		ohdrBlock.getBlockElements().add(HEXUtils.intToByteArrayLE(dataBlock.getBlockBytes().length * 0x8, 0x4)); // Create a fake last one, same as DATA size
		List<SBinDataElement> sbinDataElements = new ArrayList<>();
		List<byte[]> blockElements = new ArrayList<>();
		
		int ohdrPrevValue = 0;
		int i = 0;
		boolean isAllObjectsKnown = true;
		for (byte[] ohdrPos : ohdrBlock.getBlockElements()) {
			SBinDataElement element = new SBinDataElement();
			int elementOHDR = HEXUtils.byteArrayToInt(ohdrPos);
			int elementBegin = elementOHDR / 0x8;
			
			byte[] elementHex = Arrays.copyOfRange(dataBlock.getBlockBytes(), ohdrPrevValue, elementBegin);
			element.setOrderHexId(HEXUtils.hexToString(HEXUtils.shortToBytes(i)));
			element.setOhdrUnkRemainder(elementOHDR - (elementBegin * 0x8));
			ohdrPrevValue = elementBegin;
			
			if (!parseDATAFields(elementHex, sbinJson, i, element) && isAllObjectsKnown) {
				isAllObjectsKnown = false;
			}
			sbinDataElements.add(element);
			blockElements.add(elementHex); // For internal use
			i++;
		}
		sbinJson.setCDATAllStringsFromDATA(isAllObjectsKnown);
		sbinJson.setDataElements(sbinDataElements);
		dataBlock.setBlockElements(blockElements);
	}
	
	// TODO Re-write, looks not so great
	private boolean parseDATAFields(byte[] elementHex, SBinJson sbinJson, int i, SBinDataElement element) {
		if (LaunchParameters.isDATAObjectsUnpackDisabled()) {
			fillElementHexValue(element, elementHex);
			return false;
		}
		
		boolean isObjectKnown = false;
		int structId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 0, 2));
		SBinMapType mapType = SBinMapUtils.getMapType(elementHex, sbinJson);
		if (mapType != null) {
			processDataMap(elementHex, element, mapType, sbinJson);
			isObjectKnown = true;
		} 
		else if (i == 0 && elementHex.length == 0x12) {
			// The first DATA element is probably some unique structure, but we parse it too since it contains one of CDAT strings
			// TODO Some files have different DATA structure even with objects (e.g car configs)
			processFirstDATAEntry(elementHex, element, sbinJson.getCDATStrings());
			isObjectKnown = true;
		} 
		else if (!sbinJson.getStructs().isEmpty() && sbinJson.getStructs().size() > structId) {
			List<SBinDataField> fields = new ArrayList<>();
			SBinStruct struct = sbinJson.getStructs().get(structId);
			if (struct != null && isValidObject(structId, struct, elementHex.length)) {
				element.setStructName(struct.getName());
				element.setStructObject(true); // Struct from SBin file itself
				for (SBinField field : struct.getFieldsArray()) {
					SBinDataField dataField = new SBinDataField();
					dataField.setName(field.getName());
					dataField.setType(field.getType());
					if (field.getFieldTypeEnum() != null &&
							field.getFieldTypeEnum().equals(SBinFieldType.ENUM_ID_INT32)) {
						dataField.setEnumJsonPreview(field.getEnumJsonPreview());
						dataField.setEnumDataMapIdJsonPreview(getTempEnumDataIdValue(sbinJson, field));
					}
					
					if (field.getFieldTypeEnum() != null && 
							field.getFieldTypeEnum().equals(SBinFieldType.SUB_STRUCT)) {
						readSubStructs(sbinJson, dataField, field, elementHex, element, 0);
					} else {
						processDataFieldValue(field, null, 0, dataField, elementHex, sbinJson, element);
					}
					fields.add(dataField);
				}
				element.setFields(fields);
				isObjectKnown = true;
			}
		}
		if (!isObjectKnown) {
			fillElementHexValue(element, elementHex);
		}
		return isObjectKnown;
	}
	
	private void readSubStructs(SBinJson sbinJson, SBinDataField dataField, 
			SBinField field, byte[] elementHex, SBinDataElement element, int subStructOffset) {
		List<SBinDataField> subFields = new ArrayList<>();
		dataField.setSubStruct(field.getSubStruct());
		SBinStruct struct = DataUtils.getStructByName(sbinJson, field.getSubStruct());
		
		for (SBinField subField : struct.getFieldsArray()) {
			SBinDataField subDataField = new SBinDataField();
			subDataField.setName(subField.getName());
			subDataField.setType(subField.getType());
			
			if (subField.getFieldTypeEnum() != null && 
					subField.getFieldTypeEnum().equals(SBinFieldType.SUB_STRUCT)) {
				// there can be more than one Sub-Struct level
				if (subStructOffset == 0) {
					subStructOffset = field.getStartOffset();
				}
				readSubStructs(sbinJson, subDataField, subField, elementHex, element, subStructOffset);
			} else {
				processDataFieldValue(subField, field, subStructOffset, subDataField, elementHex, sbinJson, element);
			}
			subFields.add(subDataField);
			if (element.getOrderHexId().contentEquals("0900")) {
				System.out.println(struct.getFieldsArray().size() + ", " + subDataField.getType() + ", " + subDataField.getValue());
			}
		}
		dataField.setSubFields(subFields);
	}
	
	private void fillElementHexValue(SBinDataElement element, byte[] elementHex) {
		element.setHexValue(HEXUtils.hexToString(elementHex));
	}
	
	// Element must be equal or longer than the sum of known Struct field sizes
	private boolean isValidObject(int structId, SBinStruct struct, int elementLength) {
		if (elementLength < 5) {return false;}
//		if (structId != 0) {return true;}
		int supposedLength = 0;
		for (SBinField field : struct.getFieldsArray()) {
			supposedLength += field.getFieldSize();
		}
		if (elementLength > supposedLength + 0x8) {return false;}
		return elementLength >= supposedLength;
	}
	
	private void processDataMap(byte[] elementHex, SBinDataElement element, SBinMapType mapType, SBinJson sbinJson) {
		List<byte[]> mapValues = readDATABlockObjectMap(elementHex, mapType.getEntrySize());
		int bytesTaken = SBinMapUtils.HEADERENTRY_SIZE * 2;
		
		if (!mapType.isStringDataMap()) {
			List<String> mapElements = new ArrayList<>();
			SBinCDATEntry stringObj = null;
			for (byte[] mapValue : mapValues) {
				if (mapType.isCDATEntries()) {
					stringObj = getCDATEntryByShortCHDRId(mapValue, 0, 2, sbinJson.getCDATStrings());
					mapElements.add(stringObj.getString());
				} else {
					mapElements.add(HEXUtils.hexToString(Arrays.copyOf(mapValue, 2)));
				}
				bytesTaken += mapValue.length;
			}
			element.setMapElements(mapElements);
			// Enum strings placement can be varied and important, to repack the SBin 1-to-1 to the original
			if (mapType.isEnumMap() && stringObj != null && 
					sbinJson.getCDATStrings().indexOf(stringObj) == (sbinJson.getCDATStrings().size() - 1)) {
				sbinJson.setENUMMidDATAStringsOrdering(false);
			}
		} else { // StringData
			List<SBinStringPair> strDataElements = new ArrayList<>();
			for (byte[] mapValue : mapValues) {
				SBinStringPair newStr = new SBinStringPair();
				newStr.setStringId(getCDATStringByShortCHDRId(mapValue, 0, 2, sbinJson.getCDATStrings()));
				newStr.setString(getCDATStringByShortCHDRId(mapValue, 2, 4, sbinJson.getCDATStrings())); 
				newStr.setHalVersionValue(HEXUtils.byteArrayToInt(Arrays.copyOfRange(mapValue, 4, 8)));
				strDataElements.add(newStr);
				bytesTaken += mapValue.length;
			}
			element.setStringData(strDataElements);
		}
		element.setStructName(mapType.getTypeName());
		element.setExtraHexValue(HEXUtils.hexToString(
				Arrays.copyOfRange(elementHex, bytesTaken, elementHex.length)));
	}
	
	private void processFirstDATAEntry(byte[] elementHex, SBinDataElement element, List<SBinCDATEntry> cdatStrings) {
		element.setStructName(FIRST_DATA_STRUCTNAME);
		List<SBinDataField> fields = new ArrayList<>();
		
		SBinDataField hex1 = new SBinDataField();
		hex1.setName("HEXData_Part1");
		hex1.setType(HEX_DATA_TYPE);
		hex1.setValue(HEXUtils.hexToString(Arrays.copyOfRange(elementHex, 0, 4)));
		hex1.setForcedHexValue(true);
		fields.add(hex1);
		
		SBinDataField cdatString = new SBinDataField();
		cdatString.setName("CDATString");
		cdatString.setType("CHDR_ID_REF");
		cdatString.setValue(getCDATStringByShortCHDRId(elementHex, 4, 6, cdatStrings));
		fields.add(cdatString);
		
		SBinDataField hex2 = new SBinDataField();
		hex2.setName("HEXData_Part2");
		hex2.setType(HEX_DATA_TYPE);
		hex2.setValue(HEXUtils.hexToString(Arrays.copyOfRange(elementHex, 6, elementHex.length)));
		hex2.setForcedHexValue(true);
		fields.add(hex2);
		element.setFields(fields);
	}
	
	private void processDataFieldValue(SBinField field, SBinField parentField, int subStructOffset,
			SBinDataField dataField, byte[] elementHex, SBinJson sbinJson, SBinDataElement element) {
		// First two bytes is taken by Struct ID, start byte goes after
		int startOffset = field.getStartOffset() + 2;
		if (parentField != null) {
			startOffset += parentField.getStartOffset() + subStructOffset;
		}
		int fieldRealSize = field.getFieldSize();
		if (field.isDynamicSize()) {
			if (parentField != null && !parentField.isDynamicSize()) { // If not last, we could get full Struct size from parent Struct
				fieldRealSize = parentField.getFieldSize() - field.getStartOffset();
			} else {
				fieldRealSize = elementHex.length - startOffset;
				int fieldStandardSize = SBinEnumUtils.getFieldStandardSize(field.getFieldTypeEnum());
				
				if (field.getFieldTypeEnum() != null && fieldStandardSize < fieldRealSize) {
					int paddingSize = elementHex.length - startOffset - fieldStandardSize;
					fieldRealSize -= paddingSize;
					element.setExtraHexValue(HEXUtils.hexToString(
							Arrays.copyOfRange(elementHex, startOffset + fieldRealSize, elementHex.length)));
				} 
			}
		}
		try { // Some files like layouts.sb is just too complex & different
			byte[] valueHex = Arrays.copyOfRange(elementHex, startOffset, startOffset + fieldRealSize);
			dataField.setValue(
					SBinEnumUtils.formatFieldValueUnpack(field, dataField, fieldRealSize, valueHex, sbinJson));
		} catch(ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
			handleUnknownDATAHex(field, fieldRealSize, dataField, elementHex, element.getOrderHexId());
		}
	}
	
	private void handleUnknownDATAHex(SBinField field, int fieldRealSize, 
			SBinDataField dataField, byte[] elementHex, String hexId) {
		System.out.println("!!! Unsupported DATA object. Output file is not suitable for repack. Info: Hex ID: " + hexId + ", field: " 
				+ field.getName() + ", type: " + field.getType() + ", startOffset: " + field.getStartOffset() 
				+ ", fieldRealSize: " + fieldRealSize + ", dynamicSize: " + field.isDynamicSize() + ", hex size: " + elementHex.length
				+ ", hex: " + HEXUtils.hexToString(elementHex));
		dataField.setType(null);
		dataField.setEnumJsonPreview(null);
		dataField.setForcedHexValue(true);
		dataField.setValue("!pls fix!");
	}
	
	private void readEnumHeaders(SBinJson sbinJson, SBinBlockObj enumBlock) {
		if (enumBlock.getBlockSizeInt() == 0) {
			sbinJson.setENUMMidDATAStringsOrdering(false);
			return;
		}
		
		int i = 0;
		for (byte[] enumBytes : enumBlock.getBlockElements()) {
			SBinEnum enumObj = new SBinEnum();
			enumObj.setId(i);
			enumObj.setName( // int here but anyway
					getCDATStringByShortCHDRId(enumBytes, 0, 2, sbinJson.getCDATStrings()));
			byte[] dataIdMapRef = Arrays.copyOfRange(enumBytes, 4, 6);
			enumObj.setDataIdMapRef(HEXUtils.hexToString(dataIdMapRef));
			sbinJson.addEnum(enumObj);
			i++;
		}
	}
	
	// Not the best way to update values, but it works
	private void updateEnumRelatedObjects(SBinJson sbinJson) {
		if (LaunchParameters.isDATAObjectsUnpackDisabled()) {return;}
		
		for (SBinDataElement dataElement : sbinJson.getDataElements()) {
			if (dataElement.getFields() == null) {continue;}
			for (SBinDataField dataField : dataElement.getFields()) {
				if (dataField.getEnumJsonPreview() != null) {
					getEnumElementName(sbinJson, dataField);
				}
			}
 		}
	}
	
	private Long getTempEnumDataIdValue(SBinJson sbinJson, SBinField field) {
		return Long.valueOf(HEXUtils.twoLEByteArrayToInt(
				HEXUtils.decodeHexStr(sbinJson.getEnums().get(
						field.getSpecOrderId()).getDataIdMapRef())));
	}
	
	private void getEnumElementName(SBinJson sbinJson, SBinDataField dataField) {
		int enumElementId = Integer.parseInt(dataField.getValue());
		dataField.setValue(sbinJson.getDataElements().get(
				dataField.getEnumDataMapIdJsonPreview().intValue()).getMapElements().get(enumElementId));
		dataField.setEnumDataMapIdJsonPreview(null);
	}
	
	private void clearJsonOutputStuff(SBinJson sbinJson) {
		for (SBinStruct struct : sbinJson.getStructs()) {
			if (!struct.getFieldsArray().isEmpty()) {
				for (SBinField field : struct.getFieldsArray()) {
					field.setFieldTypeEnum(null);
				}
			}
		}
		for (SBinField emptyField : sbinJson.getEmptyFields()) {
			emptyField.setFieldTypeEnum(null);
		}
		// If we know the objects from all SBin blocks, we can properly re-build
		// the entire CDAT strings order 1-to-1 like original file. 
		// The first empty entry must be kept though, since it could be found on random blocks of the file
		if (sbinJson.isCDATAllStringsFromDATA()) {
			sbinJson.setCDATStrings(sbinJson.getCDATStrings().subList(0, 1));
		}
	}
	
	private SBinStruct getStructObject(SBinJson sbinJson, String structName) {
		for (SBinStruct struct : sbinJson.getStructs()) {
			if (struct.getName().contentEquals(structName)) {
				return struct;
			}
		}
		throw new NullPointerException("!!! One of DATA elements contains wrong referenced Struct (" + structName + ").");
	}

	//
	// SBin unpack functions
	//
	
	private List<SBinCDATEntry> prepareCDATStrings(List<byte[]> chdrEntries, byte[] cdatBytes) {
		List<SBinCDATEntry> cdatStrings = new ArrayList<>();
		int hexId = 0x0;
		for (byte[] chdrEntry : chdrEntries) {
			int cdatPos = HEXUtils.byteArrayToInt(Arrays.copyOfRange(chdrEntry, 0, 4));
			int cdatEntrySize = HEXUtils.byteArrayToInt(Arrays.copyOfRange(chdrEntry, 4, 8));
			//System.out.println("### cdatPos: " + Integer.toHexString(cdatPos) + ", cdatSize: " + Integer.toHexString(cdatEntrySize));
			
			SBinCDATEntry cdatEntry = new SBinCDATEntry();
			cdatEntry.setString(HEXUtils.utf8BytesToString(Arrays.copyOfRange(cdatBytes, cdatPos, cdatPos + cdatEntrySize)));
			cdatEntry.setChdrHexId(HEXUtils.hexToString(HEXUtils.shortToBytes(hexId)));
			cdatStrings.add(cdatEntry);
			hexId++;
		}
		return cdatStrings;
	}
	
	private void createCDATBlock(SBinJson sbinJson, SBinBlockObj block) throws IOException {
		ByteArrayOutputStream stringsHexStream = new ByteArrayOutputStream();
		List<byte[]> blockElements = new ArrayList<>();
		// Some elements could be empty, like the first one. Then data bytes begins with 00 splitter byte
		HEXUtils.addCDATElementsToByteArraysList(sbinJson.getCDATStrings(), blockElements);
		
		block.setBlockElements(blockElements); // Save it for CHDR block
		for (byte[] element : blockElements) {
			stringsHexStream.write(element);
			stringsHexStream.write(new byte[1]); // Zero byte splitter after each entry. Also file ends with empty zero byte
		}
		block.setBlockBytes(stringsHexStream.toByteArray());
	}
	
	private void unpackPlaylistsData(SBinJson sbinJson, SBinBlockObj dataBlock) {
		List<byte[]> playlistsMap = readDATABlockObjectMap(dataBlock.getBlockElements().get(1), 0x4);
		List<SBinPlaylistObj> playlistsJson = new ArrayList<>();
		
		for (byte[] playlistDescPos : playlistsMap) {
			int dataIndex = HEXUtils.byteArrayToInt(playlistDescPos);
			byte[] playlistDescHex = dataBlock.getBlockElements().get(dataIndex);
			
			SBinPlaylistObj playlist = new SBinPlaylistObj();
			playlist.setOhdrDescRemainder(sbinJson.getDataElements().get(dataIndex).getOhdrUnkRemainder());
			playlist.setName(
					getCDATStringByShortCHDRId(playlistDescHex, 12, 14, sbinJson.getCDATStrings()));
			
			List<byte[]> tracks = readDATABlockObjectMap(
					dataBlock.getBlockElements().get(dataIndex + 1), 0x4);
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
	
	//
	// SBin repack functions
	//
	
	private SBinBlockObj createCHDRBlock(List<byte[]> cdatElements, SBinBlockType header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(SBinBlockType.getBytes(header));
		
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
	
	private SBinBlockObj createOHDRBlock(SBinJson sbinJson, SBinBlockObj dataBlock, SBinBlockType header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(SBinBlockType.getBytes(header));
		
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
		block.setBlockBytes(ohdrHexStream.toByteArray());
		
		setSBinBlockAttributes(block);
		return block;
	}
	
	private void createDATABlockBytes(SBinJson sbinJson, SBinBlockObj block) throws IOException {
		ByteArrayOutputStream dataHexStream = new ByteArrayOutputStream();
		int i = 0;
		for (SBinDataElement dataEntry : sbinJson.getDataElements()) {
			processDATAEntry(dataEntry, sbinJson, dataHexStream, block);
			checkForInsertEnumStrings(i, sbinJson);
			i++;
		}
		switch(sbinJson.getSBinType()) {
		case PLAYLISTS:
			dataHexStream.write(preparePlaylistsDataForSBinBlock(block, sbinJson));
			break;
		default: break;
		}
		block.setBlockBytes(dataHexStream.toByteArray());
	}
	
	// Enum strings must be placed exactly after 1st DATA entry... Sometimes.
	private void checkForInsertEnumStrings(int i, SBinJson sbinJson) {
		if (!sbinJson.isENUMMidDATAStringsOrdering() || i != 1) {return;}
		for (SBinEnum enumObj : sbinJson.getEnums()) {
			int enumMapId = HEXUtils.twoLEByteArrayToInt(HEXUtils.decodeHexStr(enumObj.getDataIdMapRef()));
			SBinDataElement dataCheck = sbinJson.getDataElements().get(enumMapId);
			for (String entry : dataCheck.getMapElements()) {
				DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), entry);
			}
		}
	}
	
	private void processDATAEntry(SBinDataElement dataEntry, SBinJson sbinJson, 
			ByteArrayOutputStream dataHexStream, SBinBlockObj block) throws IOException {
		ByteArrayOutputStream dataElementStream = new ByteArrayOutputStream();
		// Unknown object or other stuff represented as HEX array
		if (dataEntry.getFields() == null && dataEntry.getStructName() == null) {
			dataElementStream.write(HEXUtils.decodeHexStr(dataEntry.getHexValue()));
		} 
		else if (dataEntry.isStructObject()) { // Object from SBin
			SBinStruct struct = getStructObject(sbinJson, dataEntry.getStructName());
			dataElementStream.write(HEXUtils.shortToBytes((short)struct.getId()));
			
			for (SBinDataField dataField : dataEntry.getFields()) {
				if (dataField.getSubStruct() != null) {
					buildSubStructEntry(dataEntry.getOrderHexId(), sbinJson, dataField, dataElementStream);
				} else {
					dataElementStream.write(fieldValueToBytes(dataField, struct, sbinJson));
				}
			}
		} 
		else if (dataEntry.getStructName().contentEquals(FIRST_DATA_STRUCTNAME)) {
			for (SBinDataField dataField : dataEntry.getFields()) {
				dataElementStream.write(dataField.getType().contentEquals(HEX_DATA_TYPE)
						? HEXUtils.decodeHexStr(dataField.getValue())
						: DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), dataField.getValue()));
			}
		}
		else { // Map or Enum
			buildDATAMapEntry(dataEntry, sbinJson, dataElementStream);
		}
		if (dataEntry.getExtraHexValue() != null) {
			dataElementStream.write(HEXUtils.decodeHexStr(dataEntry.getExtraHexValue()));
		}
		byte[] dataEntryHex = dataElementStream.toByteArray();
		dataHexStream.write(dataEntryHex);
		//System.out.println("id: " + dataEntry.getOrderHexId() + ", length: " + dataEntryHex.length + ", bytes: " + HEXUtils.hexToString(dataEntryHex));
		block.addToOHDRMapTemplate(dataEntryHex.length, dataEntry.getOhdrUnkRemainder());
	}
	
	private void buildDATAMapEntry(SBinDataElement dataEntry, 
			SBinJson sbinJson, ByteArrayOutputStream dataElementStream) throws IOException {
		SBinMapType mapType = SBinMapUtils.getMapType(dataEntry.getStructName());
		dataElementStream.write(HEXUtils.intToByteArrayLE(mapType.getTypeId(), 0x4));
		
		if (!mapType.isStringDataMap()) {
			dataElementStream.write(HEXUtils.intToByteArrayLE(dataEntry.getMapElements().size(), 0x4));
			for (String mapEntry : dataEntry.getMapElements()) {
				byte[] value = mapType.isCDATEntries() 
						? DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), mapEntry)
						: HEXUtils.decodeHexStr(mapEntry);
				dataElementStream.write(value);
				dataElementStream.write(new byte[mapType.getEntrySize() - value.length]);
			}
		} else {
			dataElementStream.write(HEXUtils.intToByteArrayLE(dataEntry.getStringData().size(), 0x4));
			for (SBinStringPair pair : dataEntry.getStringData()) {
				dataElementStream.write(DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), pair.getStringId()));
				dataElementStream.write(DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), pair.getString()));
				dataElementStream.write(HEXUtils.intToByteArrayLE(pair.getHalVersionValue(), 0x4));
			}
		}
	}
	
	private byte[] fieldValueToBytes(SBinDataField dataField, SBinStruct struct, SBinJson sbinJson) throws IOException {
		ByteArrayOutputStream dataFieldStream = new ByteArrayOutputStream();
		if (dataField.isForcedHexValue()) {
			dataFieldStream.write(HEXUtils.decodeHexStr(dataField.getValue()));
		} else { // Non-HEX value here means that we know it's type
			SBinFieldType valueType = SBinFieldType.valueOf(dataField.getType());
			int fieldRealSize = SBinEnumUtils.getFieldStandardSize(valueType); 
			for (SBinField field : struct.getFieldsArray()) {
				if (field.getName().contentEquals(dataField.getName()) && !field.isDynamicSize()) {
					fieldRealSize = field.getFieldSize();
					//System.out.println(dataField.getName() + ", fieldRealSize: " + fieldRealSize);
				}
			}
			byte[] convertedValue = SBinEnumUtils.convertValueByType(valueType, dataField, sbinJson, fieldRealSize);
			dataFieldStream.write(convertedValue);
			if (convertedValue.length != fieldRealSize) {
				//System.out.println(dataField.getType() + ", " + dataField.getName() + ", add: " + (fieldRealSize - convertedValue.length));
				dataFieldStream.write(new byte[fieldRealSize - convertedValue.length]);
			} 
		}
		return dataFieldStream.toByteArray();
	}
	
	private void buildSubStructEntry(String hexId, SBinJson sbinJson, SBinDataField dataField, ByteArrayOutputStream dataElementStream) throws IOException {
		SBinStruct subStruct = getStructObject(sbinJson, dataField.getSubStruct());
		for (SBinDataField subField : dataField.getSubFields()) {
			if (subField.getSubStruct() != null) {
				buildSubStructEntry(hexId, sbinJson, subField, dataElementStream);
			} else {
				if (hexId.contentEquals("0900")) {
					System.out.println(subField.getType());
				}
				dataElementStream.write(fieldValueToBytes(subField, subStruct, sbinJson));
			}
		}
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
			playlistHex.setName(DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), playlist.getName()));
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
				trackEntry.setFilePath(DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), track.getFilePath()));
				trackEntry.setUnkHex2(PLAYLISTS_DATA_TRACK_UNK2);
				trackEntry.setArtist(DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), track.getArtist()));
				trackEntry.setUnkHex3(PLAYLISTS_DATA_TRACK_UNK3);
				trackEntry.setTitle(DataUtils.processStringInCDAT(sbinJson.getCDATStrings(), track.getTitle()));
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
