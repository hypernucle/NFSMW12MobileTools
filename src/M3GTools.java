import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import util.HEXClasses.*;
import util.HEXUtils;
import util.LogEntity;
import util.M3GObjectType;

// M3G information based on "M3G2FBX" tool by RaduMC
public class M3GTools {
	
	private static final byte[] IM2M3G_HEADER = HEXUtils.decodeHexStr("AB494D324D3347BB"); // «IM2M3G»
	private static final byte[] IM2M3G_HEADER_PART2 = HEXUtils.decodeHexStr("0D0A1A0A");
	
	private static final int IM2M3G_HEADER_SIZE = 0xC;
	private static final int IM2M3G_FILESIZESPART_SIZE = 0x9;

	private static int curPos = 0x0;
	
	private static final Logger jl = Logger.getLogger(LogEntity.class.getSimpleName());
	
	public void mapM3G(String filePath) throws IOException {
		Path modelFilePath = Paths.get(filePath);
		byte[] m3gBytes = null;
		try {
			m3gBytes = Files.readAllBytes(modelFilePath);
		} catch (NoSuchFileException noFile) {
			jl.log(Level.SEVERE, "File cannot be found ({0}), aborted.", filePath);
			return;
		}

		if (!Arrays.equals(Arrays.copyOfRange(m3gBytes, 0x0, 0x8), IM2M3G_HEADER)) {
			jl.log(Level.SEVERE, "This M3G version is not supported, version IM2 required.");
			return;
		}
		if (!Arrays.equals(Arrays.copyOfRange(m3gBytes, 0x8, IM2M3G_HEADER_SIZE), IM2M3G_HEADER_PART2)) {
			jl.log(Level.WARNING, "M3G header 2nd part is unusual, various issues might be expected.");
		}
		
		StringBuilder strLog = new StringBuilder();
		M3GModel m3g = new M3GModel();
		m3g.setHeader(Arrays.copyOfRange(m3gBytes, 0x0, IM2M3G_HEADER_SIZE));
		changeCurPos(IM2M3G_HEADER_SIZE);
		
		byte[] fileSizes = getBytesFromCurPos(m3gBytes, IM2M3G_FILESIZESPART_SIZE);
		m3g.setIsCompressed(fileSizes[0]);
		m3g.setFileSize(Arrays.copyOfRange(fileSizes, 0x1, 0x5)); // Including file sizes and excluding header
		m3g.setUncompressedFileSize(Arrays.copyOfRange(fileSizes, 0x5, IM2M3G_FILESIZESPART_SIZE));
		changeCurPos(IM2M3G_FILESIZESPART_SIZE);
		int i = 1; // Header counts too
		
		while (m3gBytes.length - getCurPos() != 0x4) { // Ending empty part (or possible checksum)
			readNextObject(m3gBytes, m3g, i, strLog);
			i++;
		}
		
		jl.log(Level.INFO, "Map log has been saved, object count: " + i + ".");
		// TODO better file name
		Files.write(Paths.get(filePath + ".map.txt"), strLog.toString().getBytes(StandardCharsets.UTF_8));
	}
	
	//
	//
	//
	
	private void readNextObject(byte[] m3gBytes, M3GModel m3g, int i, StringBuilder strLog) {
		M3GObjGeneric obj = new M3GObjGeneric();
		int beginAddr = getCurPos();
		obj.setType(getByteFromCurPos(m3gBytes));
		changeCurPos(0x1);
		obj.setSize(getBytesFromCurPos(m3gBytes, 0x4));
		changeCurPos(0x4);
		byte[] firstObjBytes = getBytesFromCurPos(m3gBytes, obj.getSize());
		obj.setData(firstObjBytes);
		m3g.addObject(obj);
		
		changeCurPos(obj.getSize());
		int endAddr = getCurPos();
		M3GObjectType objTypeEnum = M3GObjectType.valueOf(obj.getType());
		String objTypeStr = objTypeEnum != null ? objTypeEnum.toString() 
				: "0x" + HEXUtils.byteToHexString(obj.getTypeByte());
		strLog.append(String.format("Object #%d type: %s, size: %d (begin addr: 0x%s, end addr: 0x%s)%n", 
				i, objTypeStr, obj.getSize(), HEXUtils.hexToString(HEXUtils.intToByteArrayBE(beginAddr)), 
				HEXUtils.hexToString(HEXUtils.intToByteArrayBE(endAddr)) ));
	}
	
	//
	// Util methods
	//
	
	public static int getCurPos() {
		return curPos;
	}
	private static void changeCurPos(int addition) {
		curPos = curPos + addition;
	}
	public static void setCurPos(int newPos) {
		curPos = newPos;
	}
	private byte[] getBytesFromCurPos(byte[] data, int to) {
		return Arrays.copyOfRange(data, curPos, curPos + to);
	}
	private byte getByteFromCurPos(byte[] data) {
		return data[curPos];
	}
}
