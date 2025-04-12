import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import util.FNV1;
import util.SBinType;
import util.DataClasses.*;
import util.DataUtils;
import util.HEXClasses.*;
import util.HEXUtils;
import util.LaunchParameters;
import util.SBJson;
import util.SBinBlockType;
import util.SBinDataGlobalType;
import util.SBinEnumUtils;
import util.SBinFieldType;
import util.SBinHCStructs;
import util.SBinMapUtils;
import util.SBinMapUtils.SBinMapType;
import util.TextureUtils;

public class SBin {
	
	private static final byte[] SHORTBYTE_EMPTY = new byte[2];
	//
	private static final byte[] PLAYLISTS_DATA_DESC_UNK1 = HEXUtils.decodeHexStr("1C0002000D000C000000");
	private static final byte[] PLAYLISTS_DATA_DESC_UNK2 = HEXUtils.decodeHexStr("04000F00180000000000");
	private static final byte[] PLAYLISTS_DATA_TRACK_UNK1 = HEXUtils.decodeHexStr("220005000D000C000000");
	private static final byte[] PLAYLISTS_DATA_TRACK_UNK2 = HEXUtils.decodeHexStr("07000D0016000000");
	private static final byte[] PLAYLISTS_DATA_TRACK_UNK3 = HEXUtils.decodeHexStr("08000D0020000000");
	
	private static int curPos = 0x0;
	
	//
	
	public static void startup(String[] args) throws IOException {
		SBJson.initNewSBJson();
		LaunchParameters.checkLaunchParameters(args);
		SBinMapUtils.initMapTypes();
	}

	public Checksum unpackSBin(String fileType, String filePath, boolean output) throws IOException, InterruptedException {
		Path sbinFilePath = Paths.get(filePath);
		byte[] sbinData = Files.readAllBytes(sbinFilePath);

		int sbinVersion = sbinData[4];
		if (sbinVersion != 0x03) {
			System.out.println("!!! This SBin version is not supported, version 3 required.");
			return null;
		}
		if (SBJson.get().getSBinType() == null) {
			SBJson.get().setSBinType(SBinType.valueOf(fileType.toUpperCase()));
		}
		SBJson.get().setFileName(sbinFilePath.getFileName().toString());
		SBJson.get().setSBinVersion(sbinVersion);
		
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
				SBJson.get().getSBinType() == SBinType.TEXTURE ? SBinBlockType.BULK : null);
		prepareCDATStrings(chdrBlock.getBlockElements(), cdatBlock.getBlockBytes());
		
		if (LaunchParameters.isDATAObjectsUnpackDisabled()) {
			SBJson.get().setENUMHexStr(HEXUtils.hexToString(enumBlock.getBlockBytes()).toUpperCase());
			SBJson.get().setSTRUHexStr(HEXUtils.hexToString(struBlock.getBlockBytes()).toUpperCase());
			SBJson.get().setFIELHexStr(HEXUtils.hexToString(fielBlock.getBlockBytes()).toUpperCase());
		} else {
			readEnumHeaders(enumBlock);
			readStructsAndFields(struBlock, fielBlock);
		}
		parseDATABlock(ohdrBlock, dataBlock);
		updateEnumRelatedObjects();
		// Used for separate file editors, not all of .sb files gets proper objects layouts
		switch(SBJson.get().getSBinType()) {
		case PLAYLISTS: // TODO Re-write to new HC Structs system
			unpackPlaylistsData(dataBlock);
			break;
		case TEXTURE:
			// BULK: Image mipmap offsets
			SBinBlockObj bulkBlock = processSBinBlock(sbinData, SBinBlockType.BULK, SBinBlockType.BARG);
			// BARG: Image plain data
			SBinBlockObj bargBlock = processSBinBlock(sbinData, SBinBlockType.BARG, null);
			TextureUtils.extractImage(bulkBlock, bargBlock.getBlockBytes());
			break;
		default: break;
		}
		SBJson.clearJsonOutputStuff();
		if (output) {
			SBJson.outputSBJson();
		} else {
			return getFileBytesChecksum(sbinData);
		}
		return null;
	}

	public Checksum repackSBin(String filePath, boolean output) throws IOException {
		if (output) { // Already loaded during FileCheck
			SBJson.loadSBJson(filePath);
		}
		if (SBJson.get().getSBinType() == SBinType.TEXTURE) {
			TextureUtils.checkForImageFormatOperations();
		}

		// ENUM
		SBinBlockObj enumBlock = createSBinBlock(SBinBlockType.ENUM);
		// STRU & FIEL
		SBinBlockObj struBlock = new SBinBlockObj();
		SBinBlockObj fielBlock = new SBinBlockObj();
		createSTRUFIELBlocks(struBlock, fielBlock);
		// OHDR & DATA
		SBinBlockObj dataBlock = createSBinBlock(SBinBlockType.DATA);
		SBinBlockObj ohdrBlock = createOHDRBlock(dataBlock, SBinBlockType.OHDR);
		// CHDR & CDAT
		SBinBlockObj cdatBlock = createSBinBlock(SBinBlockType.CDAT);
		SBinBlockObj chdrBlock = createCHDRBlock(cdatBlock.getBlockElements(), SBinBlockType.CHDR);
		
		ByteArrayOutputStream additionalBlocksStream = new ByteArrayOutputStream();
		if (SBJson.get().getSBinType() == SBinType.TEXTURE) {
			// BULK & BARG
			createBULKBARGBlocks(additionalBlocksStream);
		} 
		ByteArrayOutputStream fileOutputStr = new ByteArrayOutputStream();
		fileOutputStr.write(SBinBlockType.getBytes(SBinBlockType.SBIN));
		fileOutputStr.write(HEXUtils.intToByteArrayLE(SBJson.get().getSBinVersion()));
		
		fileOutputStr.write(buildSBinBlock(enumBlock));
		fileOutputStr.write(buildSBinBlock(struBlock));
		fileOutputStr.write(buildSBinBlock(fielBlock));
		fileOutputStr.write(buildSBinBlock(ohdrBlock));
		fileOutputStr.write(buildSBinBlock(dataBlock));
		fileOutputStr.write(buildSBinBlock(chdrBlock));
		fileOutputStr.write(buildSBinBlock(cdatBlock));
		fileOutputStr.write(additionalBlocksStream.toByteArray());
		
		byte[] fileBytes = fileOutputStr.toByteArray();
		if (output) {
			Files.write(Paths.get("new_" + SBJson.get().getFileName()), fileBytes);
		}
		return getFileBytesChecksum(fileBytes);
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

	private SBinBlockObj createSBinBlock(SBinBlockType header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(SBinBlockType.getBytes(header));
		
		switch(header) {
		case ENUM:
			block.setBlockBytes(createENUMBlockBytes());
			break;
		case OHDR:
			break;
		case DATA:
			createDATABlockBytes(block);
			break;
		case CHDR:
			break;
		case CDAT:
			createCDATBlock(block);
			if (!SBJson.get().getSBinType().equals(SBinType.TEXTURE)) {
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
	
	private byte[] createENUMBlockBytes() throws IOException {
		ByteArrayOutputStream enumHexStream = new ByteArrayOutputStream();
		if (SBJson.get().getENUMHexStr() != null) {
			enumHexStream.write(HEXUtils.decodeHexStr(SBJson.get().getENUMHexStr()));
		}
		else for (SBinEnum enumEntry : SBJson.get().getEnums()) {
			enumHexStream.write(DataUtils.processStringInCDAT(enumEntry.getName()));
			enumHexStream.write(SHORTBYTE_EMPTY);
			enumHexStream.write(HEXUtils.decodeHexStr(enumEntry.getDataIdMapRef()));
			enumHexStream.write(SHORTBYTE_EMPTY);
		} 
		return enumHexStream.toByteArray();
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
	
	private void createBULKBARGBlocks(ByteArrayOutputStream additionalBlocksStream) throws IOException {
		SBinBlockObj bargBlock = new SBinBlockObj();
		bargBlock.setHeader(SBinBlockType.getBytes(SBinBlockType.BARG));
		TextureUtils.repackImage(bargBlock);
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
		block.setFnv1Hash(HEXUtils.intToByteArrayLE(FNV1.hash32(block.getBlockBytes()))); 
		if (block.getBlockBytes().length != 0) {
			block.setBlockSize(HEXUtils.intToByteArrayLE(block.getBlockBytes().length));
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
	
	
	
	//
	// SBin object-related functions
	//
	
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
	
	private void parseDATAFields(byte[] elementHex, int i, SBinDataElement element) throws IOException {
		switch (element.getGlobalType()) {
		case MAP:
			//System.out.println(HEXUtils.hexToString(elementHex));
			processDataMap(elementHex, element, SBinMapUtils.getMapType(element.getStructName()));
			break;
		case STRUCT:
			List<SBinDataField> fields = new ArrayList<>();
			SBinStruct struct = DataUtils.getStructByName(element.getStructName());
			for (SBinField field : struct.getFieldsArray()) {
				SBinDataField dataField = new SBinDataField();
				dataField.setName(field.getName());
				dataField.setType(field.getType());
				getFieldSize(dataField, field, elementHex.length - 0x2);
				if (field.getFieldTypeEnum() != null &&
						field.getFieldTypeEnum().equals(SBinFieldType.ENUM_ID_INT32)) {
					dataField.setEnumJsonPreview(field.getEnumJsonPreview());
					dataField.setEnumDataMapIdJsonPreview(getTempEnumDataIdValue(field));
				}
				
				if (field.getFieldTypeEnum() != null && 
						field.getFieldTypeEnum().equals(SBinFieldType.SUB_STRUCT)) {
					readSubStructs(dataField, field, elementHex, element, 0, dataField.getFieldSize());
				} else {
					processDataFieldValue(field, null, 0, dataField, elementHex, element);
				}
				fields.add(dataField);
			}
			element.setFields(fields);
			break;
		default: // UNKNOWN
			fillElementHexValue(element, elementHex);
			break;
		}
	}
	
	private void getFieldSize(SBinDataField dataField, SBinField field, int elementSize) {
		int fieldSize = field.getFieldSize();
		if (field.isDynamicSize()) { // Fields with unknown size is always last
			fieldSize = elementSize - field.getStartOffset();
		}
		dataField.setFieldSize(fieldSize);
	}
	
	private void readSubStructs(SBinDataField dataField, 
			SBinField field, byte[] elementHex, SBinDataElement element, int subStructOffset, int rootStructSize) {
		List<SBinDataField> subFields = new ArrayList<>();
		dataField.setSubStruct(field.getSubStruct());
		SBinStruct struct = DataUtils.getStructByName(field.getSubStruct());
		
		for (SBinField subField : struct.getFieldsArray()) {
			SBinDataField subDataField = new SBinDataField();
			subDataField.setName(subField.getName());
			subDataField.setType(subField.getType());
			getFieldSize(subDataField, subField, dataField.getFieldSize());
			
			if (subField.getFieldTypeEnum() != null && 
					subField.getFieldTypeEnum().equals(SBinFieldType.SUB_STRUCT)) {
				// there can be more than one Sub-Struct level
				if (subStructOffset == 0) {
					subStructOffset = field.getStartOffset();
				}
				readSubStructs(subDataField, subField, elementHex, element, subStructOffset, rootStructSize);
			} else {
				processDataFieldValue(subField, field, subStructOffset, subDataField, elementHex, element);
			}
			subFields.add(subDataField);
//			if (element.getOrderHexId().contentEquals("0900")) {
//				System.out.println(struct.getFieldsArray().size() + ", " + subDataField.getType() + ", " + subDataField.getValue());
//			}
		}
		dataField.setSubFields(subFields);
	}
	
	private void fillElementHexValue(SBinDataElement element, byte[] elementHex) {
		element.setHexValue(HEXUtils.hexToString(elementHex));
	}
	
	// Element must be equal or longer than the sum of known Struct field sizes
	private boolean isValidObject(int structId, SBinStruct struct, int elementLength) {
		if (elementLength < 3 || (structId == 0 && elementLength < 5)) {return false;}
//		if (structId != 0) {return true;}
		int supposedLength = 0;
		for (SBinField field : struct.getFieldsArray()) {
			supposedLength += field.getFieldSize();
		}
		if (elementLength > supposedLength + 0x12) {return false;}
		return elementLength >= supposedLength;
	}
	
	private void processDataFieldValue(SBinField field, SBinField parentField, int subStructOffset,
			SBinDataField dataField, byte[] elementHex, SBinDataElement element) {
		// First two bytes is taken by Struct ID, start byte goes after
		int startOffset = field.getStartOffset() + 2;
		if (parentField != null) {
			startOffset += parentField.getStartOffset() + subStructOffset;
		}

		try { // Some files can be just too complex and/or different. Should not happen though
			byte[] valueHex = Arrays.copyOfRange(elementHex, startOffset, startOffset + dataField.getFieldSize());
			dataField.setValue(SBinEnumUtils.formatFieldValueUnpack(field, dataField, valueHex));
			//
		} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
			handleUnknownDATAHex(field, dataField.getFieldSize(), dataField, elementHex, element.getOrderHexId());
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
	
	private void readEnumHeaders(SBinBlockObj enumBlock) {
		if (enumBlock.getBlockSizeInt() == 0) {
			SBJson.get().setENUMMidDATAStringsOrdering(false);
			return;
		}
		
		int i = 0;
		for (byte[] enumBytes : enumBlock.getBlockElements()) {
			SBinEnum enumObj = new SBinEnum();
			enumObj.setId(i);
			enumObj.setName( // int here but anyway
					DataUtils.getCDATStringByShortCHDRId(enumBytes, 0, 2));
			byte[] dataIdMapRef = Arrays.copyOfRange(enumBytes, 4, 6);
			enumObj.setDataIdMapRef(HEXUtils.hexToString(dataIdMapRef));
			SBJson.get().addEnum(enumObj);
			i++;
		}
	}
	
	// Not the best way to update values, but it works
	private void updateEnumRelatedObjects() {
		if (LaunchParameters.isDATAObjectsUnpackDisabled()) {return;}
		
		for (SBinDataElement dataElement : SBJson.get().getDataElements()) {
			if (dataElement.getArrayObjects() != null) {
				for (SBinDataElement arrayElement : dataElement.getArrayObjects()) {
					updateEnumValuesOnFields(arrayElement);
				}
			} else {
				updateEnumValuesOnFields(dataElement);
			}
 		}
	}
	
	private void updateEnumValuesOnFields(SBinDataElement dataElement) {
		if (dataElement.getFields() == null) {return;}
		for (SBinDataField dataField : dataElement.getFields()) {
			if (dataField.getEnumJsonPreview() != null) {
				getEnumElementName(dataField);
			}
		}
	}
	
	private Long getTempEnumDataIdValue(SBinField field) {
		return Long.valueOf(HEXUtils.twoLEByteArrayToInt(
				HEXUtils.decodeHexStr(SBJson.get().getEnums().get(
						field.getSpecOrderId()).getDataIdMapRef())));
	}
	
	private void getEnumElementName(SBinDataField dataField) {
		int enumElementId = Integer.parseInt(dataField.getValue());
		dataField.setValue(SBJson.get().getDataElements().get(
				dataField.getEnumDataMapIdJsonPreview().intValue()).getMapElements().get(enumElementId));
		dataField.setEnumDataMapIdJsonPreview(null);
	}
	
	
	
	private SBinStruct getStructObject(String structName) {
		for (SBinStruct struct : SBJson.get().getStructs()) {
			if (struct.getName().contentEquals(structName)) {
				return struct;
			}
		}
		throw new NullPointerException("!!! One of DATA elements contains wrong referenced Struct (" + structName + ").");
	}

	//
	// SBin unpack functions
	//
	
	private void readStructsAndFields(SBinBlockObj struBlock, SBinBlockObj fielBlock) {
		if (struBlock.getBlockSizeInt() == 0 || fielBlock.getBlockSizeInt() == 0) {return;}
		
		int firstReadableField = 0;
		int id = 0;
		for (byte[] structBytes : struBlock.getBlockElements()) {
			SBinStruct struct = new SBinStruct();
			struct.setId(id);
			struct.setName(DataUtils.getCDATStringByShortCHDRId(structBytes, 0, 2));
			
			int firstFieldId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(structBytes, 2, 4));
			int countToNextStruct = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(structBytes, 4, 6));
			struct.setSize(countToNextStruct); // Re-use of the same Fields happens to de different on various files
			if (countToNextStruct == 0) {
				countToNextStruct = 1;
			}
			if (firstReadableField == 0 && firstFieldId > 0) {
				firstReadableField = firstFieldId; // Read these unknown fields later
			}
			for (int i = 0; i < countToNextStruct; i++) {
				struct.addToFields(readField(fielBlock, i, firstFieldId, countToNextStruct));
			}
			//System.out.println("id: " + struct.getId() + ", name: " + struct.getName() + ", countToNextStruct: " + countToNextStruct + ", size real: " + struct.getFieldsArray().size());
			struct.getFieldsArray().get(struct.getFieldsArray().size() - 1).setDynamicSize(true);
			SBJson.get().addStruct(struct);
			id++;
		}
		// Do it after the initial iteration, since any other sub-struct could be mentioned
		for (SBinStruct struct : SBJson.get().getStructs()) {
			for (SBinField field : struct.getFieldsArray()) {
				if (field.getFieldTypeEnum() != null && 
						field.getFieldTypeEnum().equals(SBinFieldType.SUB_STRUCT)) {
					field.setSubStruct(SBJson.get().getStructs().get(field.getSpecOrderId()).getName());
				}
			}
		}
		processEmptyFields(firstReadableField, fielBlock);
 	}
	
	private SBinField readField(SBinBlockObj fielBlock, int i, int firstFieldId, int fieldsCount) {
		byte[] fieldHex = fielBlock.getBlockElements().get(firstFieldId + i);
		SBinField fieldObj = new SBinField();
		
		int nameCHDRId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(fieldHex, 0, 2));
		fieldObj.setName(SBJson.get().getCDATStrings().get(nameCHDRId).getString());
		fieldObj.setStartOffset(HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(fieldHex, 4, 6)));
		if (nameCHDRId == 0) {
			fieldObj.setHexValue(HEXUtils.hexToString(fielBlock.getBlockElements().get(i)));
		}
		
		// this order ID could be used for Enum IDs, and in Maps as Struct Array Id
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
			fieldObj.setEnumJsonPreview(SBJson.get().getEnums().get(fieldObj.getSpecOrderId()).getName());
		}
		return fieldObj;
	}
	
	private void processEmptyFields(int firstReadableField, SBinBlockObj fielBlock) {
		if (firstReadableField != 0) {
			for (int i = 0; i < firstReadableField; i++) {
				SBinField emptyField = readField(fielBlock, i, 0, 0);
				SBJson.get().addEmptyField(emptyField);
				
				// Does the Id 0x10 of DATA object contains Struct, or this is a Struct Array?
				if (emptyField.getFieldTypeEnum().equals(SBinFieldType.SUB_STRUCT)) {
					emptyField.setName(SBinMapUtils.STRUCT_ARRAY_RULE);
				}
			}
		}
	}
	
	private void parseDATABlock(SBinBlockObj ohdrBlock, SBinBlockObj dataBlock) throws IOException {
		ohdrBlock.getBlockElements().remove(0); // First one is always 0x1
		ohdrBlock.getBlockElements().add(HEXUtils.intToByteArrayLE(dataBlock.getBlockBytes().length * 0x8)); 
		// Create a fake last one, same as DATA size
		List<SBinDataElement> sbinDataElements = new ArrayList<>();
		List<byte[]> blockElements = new ArrayList<>();
		if (ohdrBlock.getBlockElements().size() > 0xFFFF) {
			SBJson.get().setDataLongElementIds(true);
		}
		
		int ohdrPrevValue = 0;
		int i = 0;
		for (byte[] ohdrPos : ohdrBlock.getBlockElements()) {
			SBinDataElement element = new SBinDataElement();
			int elementOHDR = HEXUtils.byteArrayToInt(ohdrPos);
			int elementEnd = elementOHDR / 0x8; // Or (next elementOHDR - elementOHDR >> 3)
			
			element.setOrderHexId(HEXUtils.setDataEntryHexId(i, SBJson.get().isDataLongElementIds()));
			byte[] elementHex = Arrays.copyOfRange(dataBlock.getBlockBytes(), ohdrPrevValue, elementEnd);
			int remainder = elementOHDR % 0x8;
			element.setOHDRPadRemainder(remainder);
			
			//System.out.println("hexId: " + element.getOrderHexId());
			detectElementStruct(elementHex, i, element);
			if (element.getGlobalType().equals(SBinDataGlobalType.UNKNOWN)) {
				fillElementHexValue(element, elementHex);
				SBJson.get().setCDATAllStringsFromDATA(false);
			} else if (!element.getGlobalType().equals(SBinDataGlobalType.HC_STRUCT)) {
				parseDATAFields(getCleanElementHex(ohdrPrevValue, elementEnd, element, 
						remainder, elementHex, dataBlock), i, element);
			}
			ohdrPrevValue = elementEnd;
			sbinDataElements.add(element);
			blockElements.add(elementHex); // For internal use
			i++;
		}
		SBJson.get().setDataElements(sbinDataElements);
		dataBlock.setBlockElements(blockElements);
	}
	
	private byte[] getCleanElementHex(int ohdrPrevValue, int elementEnd, SBinDataElement element, 
			int remainder, byte[] elementHex, SBinBlockObj dataBlock) {
		boolean beginOnBlockStart = ohdrPrevValue % 4 == 0;
		int partialLastBlockTakenBytes = elementEnd % 4;
		int paddingSize = 0x0;
		boolean doNotCalc = false;
		
		boolean partialLastBlockQuestion = false;
		if (element.getGlobalType().equals(SBinDataGlobalType.STRUCT)) {
			List<SBinField> fields = DataUtils.getStructByName(element.getStructName()).getFieldsArray();
			int lastFieldStartOffset = fields.get(fields.size() - 1).getStartOffset() + 0x2; // 2 bytes of Struct Id
			partialLastBlockQuestion = partialLastBlockTakenBytes != 0 
					&& lastFieldStartOffset == (elementHex.length - partialLastBlockTakenBytes);
			//System.out.println("LastBlockQuestion: " + (lastFieldStartOffset + 2) + ", " + (elementHex.length - partialLastBlockTakenBytes));
		}
		else if (element.getGlobalType().equals(SBinDataGlobalType.MAP)) {
			SBinMapType mapType = SBinMapUtils.getMapType(element.getStructName());
			int mapElementsCount = HEXUtils.byteArrayToInt(Arrays.copyOfRange(elementHex, 4, 8));
			if (!mapType.isStructArray()) {
				int mapByteSize = SBinMapUtils.HEADERFULL_SIZE + (mapElementsCount * mapType.getEntrySize());
				paddingSize = elementHex.length - mapByteSize;
			} 
			else if (mapElementsCount == 0) { // Empty StructArray always have the same size
				paddingSize = elementHex.length - SBinMapUtils.HEADERFULL_SIZE;
			} 
			doNotCalc = true; // ignore for StructArrays after all
			// TODO Fails to work well with StructArrays, due to missing sizes of each last element
		}
		
		boolean lastBlockPartialData = partialLastBlockTakenBytes != 0 && !partialLastBlockQuestion;
		if (!doNotCalc && remainder == 0x0 && paddingSize == 0x0 && lastBlockPartialData) {
			paddingSize = beginOnBlockStart 
					? partialLastBlockTakenBytes
					: 0x4 - partialLastBlockTakenBytes;
			if (paddingSize != 0x0 && checkIfPaddingZoneContainsSomething(elementHex, paddingSize)) {
				paddingSize = 0x0; // Hack for last Field in Sub-Struct, if it's placed on the end of Element
			}
		}
		//System.out.println("pad for " + element.getOrderHexId() + ": " + paddingSize + ", partialLastBlockTakenBytes: " + partialLastBlockTakenBytes + ", remainder: " + remainder);
		
		element.setExtraHexValue(HEXUtils.hexToString(new byte[paddingSize]));
		return Arrays.copyOfRange(dataBlock.getBlockBytes(), ohdrPrevValue, elementEnd - paddingSize);
	}
	
	private boolean checkIfPaddingZoneContainsSomething(byte[] elementHex, int paddingSize) {
		for (byte oneByte : Arrays.copyOfRange(elementHex, elementHex.length - paddingSize, elementHex.length)) {
			if (oneByte != 0x0) {
				return true;
			}
		}
		return false;
	}
	
	private void detectElementStruct(byte[] elementHex, int i, SBinDataElement element) {
		if (LaunchParameters.isDATAObjectsUnpackDisabled()) {
			return;
		}
		int structId = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(elementHex, 0, 2));
		if (cancelExceptionsForDATAElements(elementHex, i, structId) ||
				processHCStructs(elementHex, element, structId)) {
			return;
		}
		
		SBinMapType mapType = SBinMapUtils.getMapType(elementHex);
		if (mapType != null) {
			element.setStructName(mapType.getTypeName());
			//System.out.println("hex: " + HEXUtils.hexToString(elementHex));
			element.setGlobalType(SBinDataGlobalType.MAP);
		} 
		else if (!SBJson.get().getStructs().isEmpty() && SBJson.get().getStructs().size() > structId) {
			SBinStruct struct = SBJson.get().getStructs().get(structId);
			if (struct != null && isValidObject(structId, struct, elementHex.length)) {
				element.setStructName(struct.getName());
				element.setGlobalType(SBinDataGlobalType.STRUCT); // Struct from SBin file itself
			}
		}
		//System.out.println("structId: " + structId + ", :" + (sbinJson.getStructs().size() > structId));
	}
	
	private boolean processHCStructs(byte[] elementHex, SBinDataElement element, int structId) {
		if (SBinHCStructs.isExceptionForHCStructs(elementHex, structId)) {
			return false;
		}
		return SBinHCStructs.unpackHCStructs(elementHex, element, structId);
	}
	
	private void processDataMap(byte[] elementHex, SBinDataElement element, SBinMapType mapType) throws IOException {
		if (!mapType.isStructArray()) {
			List<String> mapElements = new ArrayList<>();
			SBinCDATEntry stringObj = null;
			int entrySize = SBJson.get().isDataLongElementIds() ? 0x4 : 0x2;
			
			for (byte[] mapValue : readDATABlockObjectMap(elementHex, mapType.getEntrySize())) {
				if (mapType.isCDATEntries()) { // Enums
					stringObj = DataUtils.getCDATEntryByEnumCHDRId(mapValue);
					mapElements.add(stringObj.getString());
				} else {
					mapElements.add(HEXUtils.hexToString(Arrays.copyOf(mapValue, entrySize)));
				}
			}
			element.setMapElements(mapElements);
			// Enum strings placement can be varied and important, to repack the SBin 1:1 to the original
			if (mapType.isEnumMap() && stringObj != null && 
					SBJson.get().getCDATStrings().indexOf(stringObj) == (SBJson.get().getCDATStrings().size() - 1)) {
				SBJson.get().setENUMMidDATAStringsOrdering(false);
			}
			element.setStructName(mapType.getTypeName());
		} else { // StructArray
			byte[] headerHex = Arrays.copyOfRange(elementHex, 2, 4);
			int structBaseId = HEXUtils.twoLEByteArrayToInt(headerHex);
			SBinStruct structBase = SBJson.get().getStructs().get(structBaseId);
			if (structBase == null) {
				fillElementHexValue(element, elementHex);
				element.setGlobalType(SBinDataGlobalType.UNKNOWN);
				return;
			}
			element.setStructBaseName(structBase.getName());
			int arraySize = HEXUtils.byteArrayToInt(Arrays.copyOfRange(elementHex, 4, 8));
			int structSize = arraySize != 0 ? (elementHex.length - SBinMapUtils.HEADERFULL_SIZE) / arraySize : 0;
			
			// Hack to deal with StringPairs, padding issue on other cases
			if (!SBJson.get().getStructs().get(0).getName().contentEquals("StringPair")) {
				fillElementHexValue(element, elementHex);
				element.setGlobalType(SBinDataGlobalType.UNKNOWN);
				SBJson.get().setCDATAllStringsFromDATA(false);
				return;
			}
			List<SBinDataElement> arrayObjects = new ArrayList<>();
			for (int i = 0; i < arraySize; i++) {
				int structHexStart = SBinMapUtils.HEADERFULL_SIZE + (structSize * i);
				int structHexEnd = structHexStart + structSize;
				
				ByteArrayOutputStream hexSplitStream = new ByteArrayOutputStream();
				hexSplitStream.write(headerHex);
				hexSplitStream.write(Arrays.copyOfRange(elementHex, structHexStart, structHexEnd));
				
				SBinDataElement arrayElement = new SBinDataElement();
				arrayElement.hideOHDRPadRemainder();
				arrayElement.setStructName(structBase.getName());
				arrayElement.setGlobalType(SBinDataGlobalType.STRUCT);
				parseDATAFields(hexSplitStream.toByteArray(), i, arrayElement);
				arrayObjects.add(arrayElement);
			}
			element.setArrayObjects(arrayObjects);
		}
	}
	
	private boolean cancelExceptionsForDATAElements(byte[] elementHex, int i, int structId) {
		if (SBJson.get().getSBinType().equals(SBinType.ROADBLOCK_LEVEL) && structId == 0x14) {
			return true; // Always empty
		} else if (SBJson.get().getSBinType().equals(SBinType.SKYDOME) && structId == 0x5) {
			return true; // Always empty
		}
		return false;
	}
	
	private void prepareCDATStrings(List<byte[]> chdrEntries, byte[] cdatBytes) {
		List<SBinCDATEntry> cdatStrings = new ArrayList<>();
		int hexId = 0x0;
		for (byte[] chdrEntry : chdrEntries) {
			int cdatPos = HEXUtils.byteArrayToInt(Arrays.copyOfRange(chdrEntry, 0, 4));
			int cdatEntrySize = HEXUtils.byteArrayToInt(Arrays.copyOfRange(chdrEntry, 4, 8));
			//System.out.println("### cdatPos: " + Integer.toHexString(cdatPos) + ", cdatSize: " + Integer.toHexString(cdatEntrySize));
			
			SBinCDATEntry cdatEntry = new SBinCDATEntry();
			cdatEntry.setString(HEXUtils.utf8BytesToString(Arrays.copyOfRange(cdatBytes, cdatPos, cdatPos + cdatEntrySize)));
			cdatEntry.setChdrHexId(HEXUtils.setDataEntryHexId(hexId, false));
			cdatStrings.add(cdatEntry);
			hexId++;
		}
		SBJson.get().setCDATStrings(cdatStrings);
	}
	
	private void createCDATBlock(SBinBlockObj block) throws IOException {
		ByteArrayOutputStream stringsHexStream = new ByteArrayOutputStream();
		List<byte[]> blockElements = new ArrayList<>();
		// Some elements could be empty, like the first one. Then data bytes begins with 00 splitter byte
		HEXUtils.addCDATElementsToByteArraysList(SBJson.get().getCDATStrings(), blockElements);
		
		block.setBlockElements(blockElements); // Save it for CHDR block
		for (byte[] element : blockElements) {
			stringsHexStream.write(element);
			stringsHexStream.write(new byte[1]); // Zero byte splitter after each entry. Also file ends with empty zero byte
		}
		block.setBlockBytes(stringsHexStream.toByteArray());
	}
	
	private void unpackPlaylistsData(SBinBlockObj dataBlock) {
		List<byte[]> playlistsMap = readDATABlockObjectMap(dataBlock.getBlockElements().get(1), 0x4);
		List<SBinPlaylistObj> playlistsJson = new ArrayList<>();
		
		for (byte[] playlistDescPos : playlistsMap) {
			int dataIndex = HEXUtils.byteArrayToInt(playlistDescPos);
			byte[] playlistDescHex = dataBlock.getBlockElements().get(dataIndex);
			
			SBinPlaylistObj playlist = new SBinPlaylistObj();
			playlist.setOhdrDescRemainder(SBJson.get().getDataElements().get(dataIndex).getOHDRPadRemainder());
			playlist.setName(
					DataUtils.getCDATStringByShortCHDRId(playlistDescHex, 12, 14));
			
			List<byte[]> tracks = readDATABlockObjectMap(
					dataBlock.getBlockElements().get(dataIndex + 1), 0x4);
			playlist.setOhdrStruRemainder(SBJson.get().getDataElements().get(dataIndex + 1).getOHDRPadRemainder());
			for (byte[] trackId : tracks) {
				int trackIndex = HEXUtils.byteArrayToInt(trackId);
				byte[] trackHex = dataBlock.getBlockElements().get(trackIndex);
				SBinPlaylistTrackObj trackObj = new SBinPlaylistTrackObj();
				
				trackObj.setOhdrUnkRemainder(SBJson.get().getDataElements().get(trackIndex).getOHDRPadRemainder());
				trackObj.setFilePath(
						DataUtils.getCDATStringByShortCHDRId(trackHex, 12, 14));
				trackObj.setArtist(
						DataUtils.getCDATStringByShortCHDRId(trackHex, 22, 24));
				trackObj.setTitle(
						DataUtils.getCDATStringByShortCHDRId(trackHex, 32, 34));
				playlist.addToPlaylist(trackObj);
			}
			playlistsJson.add(playlist);
		}
		SBJson.get().setPlaylistsArray(playlistsJson);
		
		SBJson.get().setDataElements(SBJson.get().getDataElements().subList(0, 1));
		// Keep only structure-related strings in CDAT output. 
		// This time string ordering is unusual, so we proceed with hard-coded position
		SBJson.get().setCDATStrings(SBJson.get().getCDATStrings().subList(0, 13));
	}
	
	//
	// SBin repack functions
	//
	
	private void createSTRUFIELBlocks(SBinBlockObj struBlock, SBinBlockObj fielBlock) throws IOException {
		struBlock.setHeader(SBinBlockType.getBytes(SBinBlockType.STRU));
		fielBlock.setHeader(SBinBlockType.getBytes(SBinBlockType.FIEL));
		
		if (SBJson.get().getSTRUHexStr() != null) {
			struBlock.setBlockBytes(HEXUtils.decodeHexStr(SBJson.get().getSTRUHexStr()));
			fielBlock.setBlockBytes(HEXUtils.decodeHexStr(SBJson.get().getFIELHexStr()));
		} else {
			ByteArrayOutputStream struHexStream = new ByteArrayOutputStream();
			ByteArrayOutputStream fielHexStream = new ByteArrayOutputStream();
			int fieldId = 0;
			// Place "empty" fields first
			for (SBinField emptyFld : SBJson.get().getEmptyFields()) {
				fielHexStream.write(HEXUtils.decodeHexStr(emptyFld.getHexValue()));
				fieldId++;
			}
			
			String nextFieldName = "";
			for (SBinStruct struct : SBJson.get().getStructs()) {
				struHexStream.write(DataUtils.processStringInCDAT(struct.getName()));
				boolean isFirstFieldPassed = false;
				if (SBJson.get().getStructs().indexOf(struct) + 1 < SBJson.get().getStructs().size()) {
					SBinStruct nextStruct = SBJson.get().getStructs().get(SBJson.get().getStructs().indexOf(struct) + 1);
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
					if (nextFieldName.contentEquals(field.getName()) && struct.getSize() == 0) {
//						System.out.println("same: " + field.getName());
						nextFieldName = "";
						continue;
					}
//					System.out.println("added: " + field.getName());
					fielHexStream.write(DataUtils.processStringInCDAT(field.getName()));
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
	
	private SBinBlockObj createCHDRBlock(List<byte[]> cdatElements, SBinBlockType header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(SBinBlockType.getBytes(header));
		
		ByteArrayOutputStream chdrHexStream = new ByteArrayOutputStream();
		int pos = 0;
		for (byte[] cdatEntry : cdatElements) {
			int chdrToCdatPos = pos;
			int chdrToCdatSize = cdatEntry.length;
			pos = pos + chdrToCdatSize + 0x1; // Zero byte-splitter
			chdrHexStream.write(HEXUtils.intToByteArrayLE(chdrToCdatPos));
			chdrHexStream.write(HEXUtils.intToByteArrayLE(chdrToCdatSize));
		}
		block.setBlockBytes(chdrHexStream.toByteArray());
		
		setSBinBlockAttributes(block);
		return block;
	}
	
	private SBinBlockObj createOHDRBlock(SBinBlockObj dataBlock, SBinBlockType header) throws IOException {
		SBinBlockObj block = new SBinBlockObj();
		block.setHeader(SBinBlockType.getBytes(header));
		
		ByteArrayOutputStream ohdrHexStream = new ByteArrayOutputStream();
		ohdrHexStream.write(HEXUtils.intToByteArrayLE(0x1)); // First element in OHDR
		int ohdrByteLength = 0x0;
		for (int i = 0; i < dataBlock.getOHDRMapTemplate().size() - 1; i++) {
			SBinOHDREntry entry = dataBlock.getOHDRMapTemplate().get(i);
			int entryLength = entry.getValue();
			// Some of OHDR entries have a small remainder in values, related to 4-byte alignment
			// Adding it as it is works well, usually
			ohdrByteLength += entryLength;
			ohdrHexStream.write(HEXUtils.intToByteArrayLE(ohdrByteLength + entry.getRemainder()));
		} // Ignore last DATA element - last OHDR entry ends on DATA length
		block.setBlockBytes(ohdrHexStream.toByteArray());
		
		setSBinBlockAttributes(block);
		return block;
	}
	
	private void createDATABlockBytes(SBinBlockObj block) throws IOException {
		ByteArrayOutputStream dataHexStream = new ByteArrayOutputStream();
		int i = 0;
		for (SBinDataElement dataEntry : SBJson.get().getDataElements()) {
			processDATAEntry(dataEntry, dataHexStream, block);
			checkForInsertEnumStrings(i);
			i++;
		}
		switch(SBJson.get().getSBinType()) {
		case PLAYLISTS:
			dataHexStream.write(preparePlaylistsDataForSBinBlock(block));
			break;
		default: break;
		}
		block.setBlockBytes(dataHexStream.toByteArray());
	}
	
	// Enum strings must be placed exactly after 1st DATA entry... Sometimes.
	private void checkForInsertEnumStrings(int i) {
		if (!SBJson.get().isENUMMidDATAStringsOrdering() || i != 1) {return;}
		for (SBinEnum enumObj : SBJson.get().getEnums()) {
			int enumMapId = HEXUtils.twoLEByteArrayToInt(HEXUtils.decodeHexStr(enumObj.getDataIdMapRef()));
			SBinDataElement dataCheck = SBJson.get().getDataElements().get(enumMapId);
			for (String entry : dataCheck.getMapElements()) {
				DataUtils.processStringInCDAT(entry);
			}
		}
	}
	
	private void processDATAEntry(SBinDataElement dataEntry,
			ByteArrayOutputStream dataHexStream, SBinBlockObj block) throws IOException {
		ByteArrayOutputStream dataElementStream = new ByteArrayOutputStream();
		
//		System.out.println("test hexId: " + dataEntry.getOrderHexId());
		switch(dataEntry.getGlobalType()) {
		case UNKNOWN:
			// Unknown object or other stuff represented as HEX array
			dataElementStream.write(HEXUtils.decodeHexStr(dataEntry.getHexValue()));
			break;
		case STRUCT: // Object from SBin
			processDATAStruct(dataEntry, dataElementStream, true);
			break;
		case HC_STRUCT: // Hardcoded struct
			dataElementStream.write(SBinHCStructs.repackHCStructs(dataEntry));
			break;
		case MAP:
			// Map or Enum
			buildDATAMapEntry(dataEntry, dataElementStream);
			break;
		}
		
		if (dataEntry.getExtraHexValue() != null) {
			dataElementStream.write(HEXUtils.decodeHexStr(dataEntry.getExtraHexValue()));
		}
		byte[] dataEntryHex = dataElementStream.toByteArray();
		dataHexStream.write(dataEntryHex);
		//System.out.println("id: " + dataEntry.getOrderHexId() + ", length: " + dataEntryHex.length + ", bytes: " + HEXUtils.hexToString(dataEntryHex));
		block.addToOHDRMapTemplate(dataEntryHex.length, dataEntry.getOHDRPadRemainder());
	}
	
	private void processDATAStruct(SBinDataElement dataEntry,
			ByteArrayOutputStream dataStream, boolean writeHeader) throws IOException {
		SBinStruct struct = getStructObject(dataEntry.getStructName());
		if (writeHeader) {
			dataStream.write(HEXUtils.shortToBytes((short)struct.getId()));
		}
		for (SBinDataField dataField : dataEntry.getFields()) {
			if (dataField.getSubStruct() != null) {
				buildSubStructEntry(dataEntry.getOrderHexId(), dataField, dataStream);
			} else {
				dataStream.write(fieldValueToBytes(dataField, struct));
			}
		}
	}
	
	private void buildDATAMapEntry(SBinDataElement dataEntry, ByteArrayOutputStream dataElementStream) throws IOException {
		SBinMapType mapType = SBinMapUtils.getMapType(dataEntry.getStructName());
		
		if (!mapType.isStructArray()) {
			dataElementStream.write(HEXUtils.intToByteArrayLE(mapType.getTypeId()));
			dataElementStream.write(HEXUtils.intToByteArrayLE(dataEntry.getMapElements().size()));
			for (String mapEntry : dataEntry.getMapElements()) {
				byte[] value = mapType.isCDATEntries() 
						? DataUtils.processStringInCDAT(mapEntry)
						: HEXUtils.decodeHexStr(mapEntry);
				dataElementStream.write(value);
				dataElementStream.write(new byte[mapType.getEntrySize() - value.length]);
			}
		} else {
			dataElementStream.write(HEXUtils.shortToBytes((short)mapType.getTypeId()));
			SBinStruct structBase = getStructObject(dataEntry.getStructBaseName());
			dataElementStream.write(HEXUtils.shortToBytes((short)structBase.getId()));
			dataElementStream.write(HEXUtils.intToByteArrayLE(dataEntry.getArrayObjects().size()));
			for (SBinDataElement arrayElement : dataEntry.getArrayObjects()) {
				processDATAStruct(arrayElement, dataElementStream, false);
			}
		}
	}
	
	private byte[] fieldValueToBytes(SBinDataField dataField, SBinStruct struct) throws IOException {
		if (dataField.isForcedHexValue()) {
			return HEXUtils.decodeHexStr(dataField.getValue());
		} 
		else { // Non-HEX value here means that we know it's type
			ByteArrayOutputStream dataFieldStream = new ByteArrayOutputStream();
			SBinFieldType valueType = SBinFieldType.valueOf(dataField.getType());
//			int fieldRealSize = SBinEnumUtils.getFieldStandardSize(valueType); 
//			for (SBinField field : struct.getFieldsArray()) {
//				if (field.getName().contentEquals(dataField.getName()) && !field.isDynamicSize()) {
//					fieldRealSize = field.getFieldSize();
//					//System.out.println(dataField.getName() + ", fieldRealSize: " + fieldRealSize);
//				}
//			}
			byte[] convertedValue = SBinEnumUtils.convertValueByType(valueType, dataField, dataField.getFieldSize());
			dataFieldStream.write(convertedValue);
//			if (convertedValue.length != dataField.getFieldSize()) {
//				//System.out.println(dataField.getType() + ", " + dataField.getName() + ", add: " + (fieldRealSize - convertedValue.length));
//				dataFieldStream.write(new byte[dataField.getFieldSize() - convertedValue.length]);
//			} 
			return dataFieldStream.toByteArray();
		}
	}
	
	private void buildSubStructEntry(String hexId, SBinDataField dataField, ByteArrayOutputStream dataElementStream) throws IOException {
		SBinStruct subStruct = getStructObject(dataField.getSubStruct());
		for (SBinDataField subField : dataField.getSubFields()) {
			if (subField.getSubStruct() != null) {
				buildSubStructEntry(hexId, subField, dataElementStream);
			} else {
//				if (hexId.contentEquals("0900")) {
//					System.out.println(subField.getType());
//				}
				dataElementStream.write(fieldValueToBytes(subField, subStruct));
			}
		}
	}
	
	private byte[] preparePlaylistsDataForSBinBlock(SBinBlockObj block) throws IOException {
		ByteArrayOutputStream playlistsDataHexStream = new ByteArrayOutputStream();
		int orderId = SBJson.get().getDataElements().size();
		
		orderId++; // Skip map entry
		SBinStructureEntryHex playlistsMap = new SBinStructureEntryHex();
		playlistsMap.setHeader(HEXUtils.intToByteArrayLE(0xF));
		playlistsMap.setSize(HEXUtils.intToByteArrayLE(SBJson.get().getPlaylistsArray().size()));
		playlistsMap.setPadding(new byte[0]);
		
		ByteArrayOutputStream playlistCollectionHexStream = new ByteArrayOutputStream();
		List<SBinOHDREntry> ohdrMapTemplate = new ArrayList<>();
		for (SBinPlaylistObj playlist : SBJson.get().getPlaylistsArray()) {
			playlistsMap.addToDataIds(HEXUtils.intToByteArrayLE(orderId)); // Add playlist to map
			orderId++;
			
			SBinPlaylistEntryHex playlistHex = new SBinPlaylistEntryHex();
			playlistHex.setOhdrDescRemainder(playlist.getOhdrDescRemainder());
			playlistHex.setOhdrStruRemainder(playlist.getOhdrStruRemainder());
			playlistHex.setHeader(HEXUtils.shortToBytes((short)0x2));
			playlistHex.setUnkHex1(PLAYLISTS_DATA_DESC_UNK1);
			playlistHex.setName(DataUtils.processStringInCDAT(playlist.getName()));
			playlistHex.setUnkHex2(PLAYLISTS_DATA_DESC_UNK2);
			playlistHex.setOrderId(HEXUtils.shortToBytes((short)orderId));
			
			SBinStructureEntryHex tracksMap = new SBinStructureEntryHex();
			tracksMap.setHeader(HEXUtils.intToByteArrayLE(0xF));
			tracksMap.setSize(HEXUtils.intToByteArrayLE(playlist.getPlaylist().size()));
			tracksMap.setPadding(new byte[0]);
			
			for (int i = 0; i < playlist.getPlaylist().size(); i++) {
				tracksMap.addToDataIds(HEXUtils.intToByteArrayLE(orderId + i + 1));
			} 
			playlistHex.setTracksMap(tracksMap);
			orderId++;
			
			for (SBinPlaylistTrackObj track : playlist.getPlaylist()) {
				SBinPlaylistTrackHex trackEntry = new SBinPlaylistTrackHex();
				trackEntry.setOhdrUnkRemainder(track.getOhdrUnkRemainder());
				trackEntry.setHeader(HEXUtils.shortToBytes((short)0x3));
				trackEntry.setUnkHex1(PLAYLISTS_DATA_TRACK_UNK1);
				trackEntry.setFilePath(DataUtils.processStringInCDAT(track.getFilePath()));
				trackEntry.setUnkHex2(PLAYLISTS_DATA_TRACK_UNK2);
				trackEntry.setArtist(DataUtils.processStringInCDAT(track.getArtist()));
				trackEntry.setUnkHex3(PLAYLISTS_DATA_TRACK_UNK3);
				trackEntry.setTitle(DataUtils.processStringInCDAT(track.getTitle()));
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

	public static void setCurPos(int newPos) {
		curPos = newPos;
		//		System.out.println("### curPos: " + Integer.toHexString(curPos));
	}
	
	private byte[] getBytesFromCurPos(byte[] data, int to) {
		return Arrays.copyOfRange(data, curPos, curPos + to);
	}
	
	public static Checksum getFileBytesChecksum(byte[] data) {
		Checksum crc = new CRC32();
        crc.update(data, 0, data.length);
        return crc;
	}
	
}
