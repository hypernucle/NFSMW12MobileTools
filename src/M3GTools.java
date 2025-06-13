import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	private StringBuilder strLog = new StringBuilder();
	
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
			readNextObject(m3gBytes, m3g, i);
			i++;
		}
		
		jl.log(Level.INFO, "Map log has been saved, object count: " + i + ".");
		// TODO better file name
		Files.write(Paths.get(filePath + ".map.txt"), strLog.toString().getBytes(StandardCharsets.UTF_8));
	}
	
	//
	//
	//
	
	private void readNextObject(byte[] m3gBytes, M3GModel m3g, int i) {
		M3GObjGeneric obj = new M3GObjGeneric();
		int beginAddr = getCurPos();
		obj.setType(passByteFromCurPos(m3gBytes));
		obj.setSize(passBytesFromCurPos(m3gBytes, 0x4));
		
		List<String> objLogCollection = new ArrayList<>();
		M3GObjectType objTypeEnum = M3GObjectType.valueOf(obj.getType());
		m3g.addObject(readObjectProperties(m3gBytes, obj, objTypeEnum, objLogCollection));
		
		int endAddr = getCurPos();
		String objTypeStr = objTypeEnum != null ? objTypeEnum.toString() 
				: "0x" + HEXUtils.byteToHexString(obj.getTypeByte());
		strLog.append(String.format("Object #%d type: %s, size: %d (begin addr: 0x%s, end addr: 0x%s)%n", 
				i, objTypeStr, obj.getSize(), HEXUtils.hexToString(HEXUtils.intToByteArrayBE(beginAddr)), 
				HEXUtils.hexToString(HEXUtils.intToByteArrayBE(endAddr)) ));
		for (String out : objLogCollection) {
			strLog.append(out);
		}
	}
	
	private M3GObject readObjectProperties(byte[] m3gBytes, M3GObjGeneric objTemp, 
			M3GObjectType objTypeEnum, List<String> objLogCollection) {
		if (objTypeEnum == null) {
			return readGenericObjData(m3gBytes, objTemp);
		}
		
		switch(objTypeEnum) {
		case APPEARANCE:
			return readGenericObjData(m3gBytes, objTemp);
		case GROUP:
			return readObjGroup(m3gBytes, objLogCollection);
		case IMAGE2D:
			return readGenericObjData(m3gBytes, objTemp);
		case INDEX_BUFFER:
			return readGenericObjData(m3gBytes, objTemp);
		case MESH_CONFIG:
			return readObjMesh(m3gBytes, objLogCollection);
		case SUB_MESH:
			return readGenericObjData(m3gBytes, objTemp);
		case TEXTURE_REF:
			return readGenericObjData(m3gBytes, objTemp);
		case VERTEX_ARRAY:
			return readGenericObjData(m3gBytes, objTemp);
		case VERTEX_BUFFER:
			return readGenericObjData(m3gBytes, objTemp);
		case HEADER_ELEMENT: case COMPOSITING_MODE: case POLYGON_MODE: default:
			return readGenericObjData(m3gBytes, objTemp);
		}
	}
	
	private M3GObject readGenericObjData(byte[] m3gBytes, M3GObjGeneric objTemp) {
		objTemp.setData(passBytesFromCurPos(m3gBytes, objTemp.getSize()));
		return objTemp;
	}
	
	//
	// Object reading classes
	//
	
	private M3GObjGroup readObjGroup(byte[] m3gBytes, List<String> objLogCollection) {
		M3GObjGroup groupObj = new M3GObjGroup();
		groupObj.setAnimationControllers(passBytesFromCurPos(m3gBytes, 0x4));
		groupObj.setAnimationTracks(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < groupObj.getAnimationTracks(); i++) {
			groupObj.addToAnimationArray(HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4)));
		}
		groupObj.setParameterCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < groupObj.getParameterCount(); i++) { // TODO there is more types of params
			M3GSubObjParameter subParam = new M3GSubObjParameter();
			subParam.setType(passBytesFromCurPos(m3gBytes, 0x4));
			subParam.setSize(passBytesFromCurPos(m3gBytes, 0x4));
			subParam.setValue(HEXUtils.utf8BytesToString(passBytesFromCurPos(m3gBytes, subParam.getSize())));
			groupObj.addToParameterArray(subParam);
		}
		groupObj.setHasComponentTransform(passByteFromCurPos(m3gBytes));
		if (groupObj.getHasComponentTransform() != 0) {
			M3GSubObjComponentTransform compTrans = new M3GSubObjComponentTransform();
			compTrans.setTranslation(new float[] {
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
			});
			compTrans.setScale(new float[] {
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
			});
			compTrans.setOrientationAngle(passBytesFromCurPos(m3gBytes, 0x4));
			compTrans.setOrientationAxisX(passBytesFromCurPos(m3gBytes, 0x4));
			compTrans.setOrientationAxisY(passBytesFromCurPos(m3gBytes, 0x4));
			compTrans.setOrientationAxisZ(passBytesFromCurPos(m3gBytes, 0x4));
			groupObj.setComponentTransform(compTrans);
		}
		groupObj.setHasGeneralTransform(passByteFromCurPos(m3gBytes)); // TODO
		if (groupObj.getHasGeneralTransform() != 0) {
			groupObj.setGeneralTransformBytes(passBytesFromCurPos(m3gBytes, 0x40));
		}
		groupObj.setUnkPart(passBytesFromCurPos(m3gBytes, 0x7));
		groupObj.setHasUnkFuncPart(passByteFromCurPos(m3gBytes)); // TODO
		groupObj.setChildCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < groupObj.getChildCount(); i++) { 
			groupObj.addToChildObjIndexArray(HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4)));
		}
		
		getObjGroupInfo(groupObj, objLogCollection);
		return groupObj;
	}
	
	private M3GObjMeshConfig readObjMesh(byte[] m3gBytes, List<String> objLogCollection) {
		M3GObjMeshConfig meshObj = new M3GObjMeshConfig();
		changeCurPos(meshObj.getPadding().length);
		meshObj.setUnkPart(passBytesFromCurPos(m3gBytes, 0xE));
		meshObj.setVertexBufferObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		meshObj.setSubMeshCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < meshObj.getSubMeshCount(); i++) {
			meshObj.addToSubMeshObjIndexArray(HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4)));
		}
		
		getObjMeshInfo(meshObj, objLogCollection);
		return meshObj;
	}
	
	//
	// Object logs
	//
	
	private void getObjGroupInfo(M3GObjGroup groupObj, List<String> objLogCollection) {
		int logCounter = 0;
		objLogCollection.add(String.format("::: AnimationControllers: %s%n", 
				HEXUtils.hexToString(groupObj.getAnimationControllers()) ));
		objLogCollection.add(String.format("::: AnimationTracks: %d%n", 
				groupObj.getAnimationTracks() ));
		objLogCollection.add(String.format("::: AnimationArray Count: %d%n", 
				groupObj.getAnimationArray().size() ));
		objLogCollection.add(String.format("::: ParameterCount: %d%n", 
				groupObj.getParameterCount() ));
		
		for (M3GSubObjParameter subParamLog : groupObj.getParameterArray()) {
			objLogCollection.add(String.format("::: Parameter #%d value: %s%n", 
					logCounter, subParamLog.getValue() ));
			logCounter++;
		}
		logCounter = 0;
		objLogCollection.add(String.format("::: Has Component Transform: %d%n", 
				groupObj.getHasComponentTransform() ));
		if (groupObj.getHasComponentTransform() != 0) {
			objLogCollection.add(String.format("::: Component Transform / Translation [%f, %f, %f]%n", 
					groupObj.getComponentTransform().getTranslation()[0],
					groupObj.getComponentTransform().getTranslation()[1],
					groupObj.getComponentTransform().getTranslation()[2] ));
			objLogCollection.add(String.format("::: Component Transform / Scale [%f, %f, %f]%n", 
					groupObj.getComponentTransform().getScale()[0],
					groupObj.getComponentTransform().getScale()[1],
					groupObj.getComponentTransform().getScale()[2] ));
			objLogCollection.add(String.format("::: Component Transform / Orientation Angle: %f%n", 
					groupObj.getComponentTransform().getOrientationAngle() ));
			objLogCollection.add(String.format("::: Component Transform / Orientation Axis X: %f%n", 
					groupObj.getComponentTransform().getOrientationAxisX() ));
			objLogCollection.add(String.format("::: Component Transform / Orientation Axis Y: %f%n", 
					groupObj.getComponentTransform().getOrientationAxisY() ));
			objLogCollection.add(String.format("::: Component Transform / Orientation Axis Z: %f%n", 
					groupObj.getComponentTransform().getOrientationAxisZ() ));
		}
		objLogCollection.add(String.format("::: Has General Transform: %d%n", 
				groupObj.getHasGeneralTransform() ));
		if (groupObj.getHasGeneralTransform() != 0) {
			objLogCollection.add(String.format("::: General Transform Bytes: %s%n", 
					HEXUtils.hexToString(groupObj.getGeneralTransformBytes()) ));
		}
		objLogCollection.add(String.format("::: Unknown Byte Part: %s%n", 
				HEXUtils.hexToString(groupObj.getUnkPart()) ));
		objLogCollection.add(String.format("::: Has Unknown Functional Part: %d%n", 
				groupObj.getHasUnkFuncPart() ));
		objLogCollection.add(String.format("::: Child Count: %d%n", 
				groupObj.getChildCount() ));
		for (Integer id : groupObj.getChildObjIndexArray()) {
			objLogCollection.add(String.format("::: Child #%d Object Ref. ID: %d%n", 
					logCounter, id ));
			logCounter++;
		}
	}
	
	private void getObjMeshInfo(M3GObjMeshConfig meshObj, List<String> objLogCollection) {
		int logCounter = 0;
		objLogCollection.add(String.format("::: Unknown Bytes: %s%n", 
				HEXUtils.hexToString(meshObj.getUnkPart()) ));
		objLogCollection.add(String.format("::: Vertex Buffer Object Ref. ID: %d%n", 
				meshObj.getVertexBufferObjIndex() ));
		objLogCollection.add(String.format("::: Sub-Mesh Count: %d%n", 
				meshObj.getSubMeshCount() ));
		for (Integer id : meshObj.getSubMeshObjIndexArray()) {
			objLogCollection.add(String.format("::: Sub-Mesh #%d Object Ref. ID: %d%n", 
					logCounter, id ));
			logCounter++;
		}
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
	private byte[] passBytesFromCurPos(byte[] data, int to) {
		byte[] bytes = Arrays.copyOfRange(data, curPos, curPos + to);
		changeCurPos(to);
		return bytes;
	}
	private byte getByteFromCurPos(byte[] data) {
		return data[curPos];
	}
	private byte passByteFromCurPos(byte[] data) {
		byte value = data[curPos];
		changeCurPos(0x1);
		return value;
	}
}
