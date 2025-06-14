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

//		if (!Arrays.equals(Arrays.copyOfRange(m3gBytes, 0x0, 0x8), IM2M3G_HEADER)) {
//			jl.log(Level.SEVERE, "This M3G version is not supported, version IM2 required.");
//			return;
//		}
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
		strLog.append(String.format("Object #%d type: %s, size: %d (0x%s - 0x%s)%n", 
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
		case ANIMATION_TRACK:
			return readObjAnimationTrack(m3gBytes, objLogCollection);
		case APPEARANCE:
			return readObjAppearance(m3gBytes, objLogCollection);
		case GROUP:
			return readObjGroup(m3gBytes, objLogCollection);
		case IMAGE2D:
			return readObjImage2D(m3gBytes, objLogCollection);
		case MESH_CONFIG:
			return readObjMesh(m3gBytes, objLogCollection);
		case SUB_MESH:
			return readObjSubMesh(m3gBytes, objLogCollection);
		case TEXTURE_REF:
			return readObjTextureRef(m3gBytes, objLogCollection);
		case VERTEX_BUFFER:
			return readObjVertexBuffer(m3gBytes, objLogCollection);
		case HEADER_ELEMENT: case ANIMATION_TRACK_SETTINGS: case COMPOSITING_MODE: 
		case POLYGON_MODE: case INDEX_BUFFER: case ANIMATION_BUFFER: 
		case VERTEX_ARRAY: default:
			return readGenericObjData(m3gBytes, objTemp);
		}
	}
	
	private M3GObject readGenericObjData(byte[] m3gBytes, M3GObjGeneric objTemp) {
		objTemp.setData(passBytesFromCurPos(m3gBytes, objTemp.getSize()));
		return objTemp;
	}
	
	private M3GSubObjParameter readSubParameterObject(byte[] m3gBytes) {
		M3GSubObjParameter subParam = new M3GSubObjParameter();
		subParam.setType(passBytesFromCurPos(m3gBytes, 0x4));
		subParam.setSize(passBytesFromCurPos(m3gBytes, 0x4));
		switch(subParam.getType()) {
		case 0x0: case 0x384:
			subParam.setStrValue(HEXUtils.utf8BytesToString(
					passBytesFromCurPos(m3gBytes, subParam.getSize())));
			break;
		case 0x2:
			M3GSubObjParameterProperty propertyObj = new M3GSubObjParameterProperty();
			propertyObj.setObjLabelOffset(passByteFromCurPos(m3gBytes));
			propertyObj.setObjLabel(HEXUtils.utf8BytesToString(
					passBytesFromCurPos(m3gBytes, propertyObj.getObjLabelOffset())));
			propertyObj.setPropertyLabelOffset(passBytesFromCurPos(m3gBytes, 0x4));
			propertyObj.setProperty(HEXUtils.utf8BytesToString(
					passBytesFromCurPos(m3gBytes, propertyObj.getPropertyLabelOffset())));
			subParam.setPropertyObj(propertyObj);
			break;
		case 0x259:
			subParam.setRGBAColorValue(new float[] {
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4))
			});
			break;
		default:
			subParam.setUnkBytes(passBytesFromCurPos(m3gBytes, subParam.getSize()));
			break;
		}
		return subParam;
	}
	
	//
	// Object reading classes
	//
	
	// 0x2
	private M3GObjAnimationTrack readObjAnimationTrack(byte[] m3gBytes, List<String> objLogCollection) {
		M3GObjAnimationTrack animationTrackObj = new M3GObjAnimationTrack();
		changeCurPos(animationTrackObj.getPadding().length);
		animationTrackObj.setAnimationBufferObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		animationTrackObj.setAnimationTrackSettingsObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		animationTrackObj.setUnkPart(passBytesFromCurPos(m3gBytes, 0x4));

		getObjAnimationTrackInfo(animationTrackObj, objLogCollection);
		return animationTrackObj;
	}
	
	// 0x3
	private M3GObjAppearance readObjAppearance(byte[] m3gBytes, List<String> objLogCollection) {
		M3GObjAppearance appearanceObj = new M3GObjAppearance();
		appearanceObj.setAnimationControllers(passBytesFromCurPos(m3gBytes, 0x4));
		appearanceObj.setAnimationTracks(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < appearanceObj.getAnimationTracks(); i++) {
			appearanceObj.addToAnimationArray(HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4)));
		}
		appearanceObj.setParameterCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < appearanceObj.getParameterCount(); i++) {
			appearanceObj.addToParameterArray(readSubParameterObject(m3gBytes));
		}
		appearanceObj.setLayer(passByteFromCurPos(m3gBytes));
		appearanceObj.setCompositingModeObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		appearanceObj.setFogObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		appearanceObj.setPolygonModeObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		appearanceObj.setMaterialObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		appearanceObj.setTextureRefCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < appearanceObj.getTextureRefCount(); i++) {
			appearanceObj.addToTextureRefObjIndexArray(HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4)));
		}

		getObjAppearanceInfo(appearanceObj, objLogCollection);
		return appearanceObj;
	}
	
	// 0x9
	private M3GObjGroup readObjGroup(byte[] m3gBytes, List<String> objLogCollection) {
		M3GObjGroup groupObj = new M3GObjGroup();
		groupObj.setAnimationControllers(passBytesFromCurPos(m3gBytes, 0x4));
		groupObj.setAnimationTracks(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < groupObj.getAnimationTracks(); i++) {
			groupObj.addToAnimationArray(HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4)));
		}
		groupObj.setParameterCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < groupObj.getParameterCount(); i++) {
			groupObj.addToParameterArray(readSubParameterObject(m3gBytes));
		}
		groupObj.setHasComponentTransform(passByteFromCurPos(m3gBytes));
		if (groupObj.getHasComponentTransform() != 0) {
			M3GSubObjComponentTransform compTrans = new M3GSubObjComponentTransform();
			compTrans.setTranslation(new float[] {
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4))
			});
			compTrans.setScale(new float[] {
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
					HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4))
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
	
	// 0xA
	private M3GObjImage2D readObjImage2D(byte[] m3gBytes, List<String> objLogCollection) {
		M3GObjImage2D image2DObj = new M3GObjImage2D();
		changeCurPos(image2DObj.getPadding().length);
		image2DObj.setParameterCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < image2DObj.getParameterCount(); i++) {
			image2DObj.addToParameterArray(readSubParameterObject(m3gBytes));
		}
		image2DObj.setUnkPostParamByte(passByteFromCurPos(m3gBytes));
		image2DObj.setTexFormatType(passByteFromCurPos(m3gBytes));
		image2DObj.setTexWidth(passBytesFromCurPos(m3gBytes, 0x4));
		image2DObj.setTexHeight(passBytesFromCurPos(m3gBytes, 0x4));
		
		image2DObj.setTexMetadataUnkBytes(passBytesFromCurPos(m3gBytes, 0x4));
		image2DObj.setTexMetadataSize(passBytesFromCurPos(m3gBytes, 0x4));
		image2DObj.setTexMetadata(passBytesFromCurPos(m3gBytes, image2DObj.getTexMetadataSize()));
		
		getObjImage2DInfo(image2DObj, objLogCollection);
		return image2DObj;
	}
	
	// 0xE
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
	
	// 0x11
	private M3GObjTextureRef readObjTextureRef(byte[] m3gBytes, List<String> objLogCollection) {
		M3GObjTextureRef textureRefObj = new M3GObjTextureRef();
		changeCurPos(textureRefObj.getPadding().length);
		textureRefObj.setUnkPart(passBytesFromCurPos(m3gBytes, 0x2));
		textureRefObj.setImage2DObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		textureRefObj.setUnkPart2(passBytesFromCurPos(m3gBytes, 0x8));

		getObjTextureRefInfo(textureRefObj, objLogCollection);
		return textureRefObj;
	}
	
	// 0x15
	private M3GObjVertexBuffer readObjVertexBuffer(byte[] m3gBytes, List<String> objLogCollection) {
		M3GObjVertexBuffer vertexBufferObj = new M3GObjVertexBuffer();
		changeCurPos(vertexBufferObj.getPadding().length);
		vertexBufferObj.setColorRGBA(passBytesFromCurPos(m3gBytes, 0x4));
		vertexBufferObj.setPositionVertexArrayObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		
		vertexBufferObj.setPositionBias(new float[] {
			HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
			HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
			HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4))
		});
		vertexBufferObj.setPositionScale(passBytesFromCurPos(m3gBytes, 0x4));
		vertexBufferObj.setNormalsVertexArrayObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		vertexBufferObj.setColorsObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		// -1 means 1 here
		int texCoordArrayCount = 
				Math.abs(HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4)));
		vertexBufferObj.setTextureCoordArrayCount(texCoordArrayCount);
		
		for (int i = 0; i < vertexBufferObj.getTextureCoordArrayCount(); i++) {
			M3GSubObjTextureCoord texCoordObj = new M3GSubObjTextureCoord();
			texCoordObj.setTextureCoordObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
			texCoordObj.setTextureCoordBias(new float[] {
				HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
				HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)), // M3G2FBX: 1 - coordBias[1]
				HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4))
			});
			texCoordObj.setTextureCoordScale(passBytesFromCurPos(m3gBytes, 0x4));
			
			vertexBufferObj.addToTextureCoordArray(texCoordObj);
		}
		vertexBufferObj.setTangentsVertexArrayObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		vertexBufferObj.setBinormalsVertexArrayObjIndex(passBytesFromCurPos(m3gBytes, 0x4));

		getObjVertexBufferInfo(vertexBufferObj, objLogCollection);
		return vertexBufferObj;
	}
	
	// 0x64
	private M3GObjSubMesh readObjSubMesh(byte[] m3gBytes, List<String> objLogCollection) {
		M3GObjSubMesh subMeshObj = new M3GObjSubMesh();
		changeCurPos(subMeshObj.getPadding().length);
		subMeshObj.setParameterCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < subMeshObj.getParameterCount(); i++) {
			subMeshObj.addToParameterArray(readSubParameterObject(m3gBytes));
		}
		subMeshObj.setIndexBufferObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		subMeshObj.setAppearanceObjIndex(passBytesFromCurPos(m3gBytes, 0x4));

		getObjSubMeshInfo(subMeshObj, objLogCollection);
		return subMeshObj;
	}
	
	//
	// Object logs
	//
	
	private void getSubObjParameterInfo(M3GSubObjParameter subParamLog, List<String> objLogCollection, int logCounter) {
		objLogCollection.add(String.format("::: Parameter #%d, type: %d%n", 
				logCounter, subParamLog.getType() ));
		switch(subParamLog.getType()) {
		case 0x0: case 0x384:
			objLogCollection.add(String.format("::: Parameter #%d, value: %s%n", 
					logCounter, subParamLog.getStrValue() ));
			break;
		case 0x2:
			objLogCollection.add(String.format("::: Parameter #%d, label: %s%n", 
					logCounter, subParamLog.getPropertyObj().getObjLabel() ));
			objLogCollection.add(String.format("::: Parameter #%d, property: %s%n", 
					logCounter, subParamLog.getPropertyObj().getProperty() ));
			break;
		case 0x259:
			objLogCollection.add(String.format("::: Parameter #%d, RGBA color value: [%f, %f, %f, %f]%n", 
					logCounter, subParamLog.getRGBAColorValue()[0],
					subParamLog.getRGBAColorValue()[1],
					subParamLog.getRGBAColorValue()[2],
					subParamLog.getRGBAColorValue()[3]));
			break;
		default:
			objLogCollection.add(String.format("::: Parameter #%d, bytes: %s%n", 
					logCounter, HEXUtils.hexToString(subParamLog.getUnkBytes()) ));
			break;
		}
	}
	
	// 0x2
	private void getObjAnimationTrackInfo(M3GObjAnimationTrack animationTrackObj, List<String> objLogCollection) {
		objLogCollection.add(String.format("::: Animation Buffer Object Ref. ID: %d%n", 
				animationTrackObj.getAnimationBufferObjIndex()));
		objLogCollection.add(String.format("::: Animation Track Settings Object Ref. ID: %d%n", 
				animationTrackObj.getAnimationTrackSettingsObjIndex()));
		objLogCollection.add(String.format("::: Unknown Part: %s%n", 
				HEXUtils.hexToString(animationTrackObj.getUnkPart())));
	}
	
	// 0x3
	private void getObjAppearanceInfo(M3GObjAppearance appearanceObj, List<String> objLogCollection) {
		int logCounter = 0;
		objLogCollection.add(String.format("::: Animation Controllers: %s%n", 
				HEXUtils.hexToString(appearanceObj.getAnimationControllers()) ));
		objLogCollection.add(String.format("::: Animation Tracks: %d%n", 
				appearanceObj.getAnimationTracks() ));
		for (Integer id : appearanceObj.getAnimationArray()) {
			objLogCollection.add(String.format("::: Animation Track #%d Object Ref. ID: %d%n", 
					logCounter, id ));
			logCounter++;
		}
		logCounter = 0;

		objLogCollection.add(String.format("::: Parameter Count: %d%n", 
				appearanceObj.getParameterCount() ));
		for (M3GSubObjParameter subParamLog : appearanceObj.getParameterArray()) {
			getSubObjParameterInfo(subParamLog, objLogCollection, logCounter);
			logCounter++;
		}
		logCounter = 0;

		objLogCollection.add(String.format("::: Layer: %s%n", appearanceObj.getLayer() ));
		objLogCollection.add(String.format("::: Compositing Mode Object Ref. ID: %d%n", 
				appearanceObj.getCompositingModeObjIndex() ));
		objLogCollection.add(String.format("::: Fog Object Ref. ID: %d%n", 
				appearanceObj.getFogObjIndex() ));
		objLogCollection.add(String.format("::: Polygon Mode Object Ref. ID: %d%n", 
				appearanceObj.getPolygonModeObjIndex() ));
		objLogCollection.add(String.format("::: Material Object Ref. ID: %d%n", 
				appearanceObj.getMaterialObjIndex() ));
		objLogCollection.add(String.format("::: Texture Ref. Count: %d%n", 
				appearanceObj.getTextureRefCount() ));
		for (Integer id : appearanceObj.getTextureRefObjIndexArray()) {
			objLogCollection.add(String.format("::: Texture Ref. #%d Object Ref. ID: %d%n", 
					logCounter, id ));
			logCounter++;
		}
	}
	
	// 0x9
	private void getObjGroupInfo(M3GObjGroup groupObj, List<String> objLogCollection) {
		int logCounter = 0;
		objLogCollection.add(String.format("::: Animation Controllers: %s%n", 
				HEXUtils.hexToString(groupObj.getAnimationControllers()) ));
		objLogCollection.add(String.format("::: Animation Tracks: %d%n", 
				groupObj.getAnimationTracks() ));
		for (Integer id : groupObj.getAnimationArray()) {
			objLogCollection.add(String.format("::: Animation Track #%d Object Ref. ID: %d%n", 
					logCounter, id ));
			logCounter++;
		}
		logCounter = 0;
		
		objLogCollection.add(String.format("::: Parameter Count: %d%n", 
				groupObj.getParameterCount() ));
		for (M3GSubObjParameter subParamLog : groupObj.getParameterArray()) {
			getSubObjParameterInfo(subParamLog, objLogCollection, logCounter);
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
	
	// 0xA
	private void getObjImage2DInfo(M3GObjImage2D image2DObj, List<String> objLogCollection) {
		int logCounter = 0;
		objLogCollection.add(String.format("::: Parameter Count: %d%n", 
				image2DObj.getParameterCount() ));
		for (M3GSubObjParameter subParamLog : image2DObj.getParameterArray()) {
			getSubObjParameterInfo(subParamLog, objLogCollection, logCounter);
			logCounter++;
		}
		objLogCollection.add(String.format("::: Unknown Byte Part: %s%n", 
				HEXUtils.byteToHexString(image2DObj.getUnkPostParamByte()) ));
		objLogCollection.add(String.format("::: Texture Format: %d%n", 
				image2DObj.getTexFormatType() ));
		objLogCollection.add(String.format("::: Texture width: %d%n", 
				image2DObj.getTexWidth()));
		objLogCollection.add(String.format("::: Texture height: %d%n", 
				image2DObj.getTexHeight()));
		
		objLogCollection.add(String.format("::: Texture Metadata Unknown bytes: %s%n", 
				HEXUtils.hexToString(image2DObj.getTexMetadataUnkBytes()) ));
		objLogCollection.add(String.format("::: Texture Metadata HEX: %s%n", 
				HEXUtils.hexToString(image2DObj.getTexMetadata()) ));
	}
	
	// 0xE
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
	
	// 0x11
	private void getObjTextureRefInfo(M3GObjTextureRef textureRefObj, List<String> objLogCollection) {
		objLogCollection.add(String.format("::: Unknown Part: %s%n", 
		    HEXUtils.hexToString(textureRefObj.getUnkPart())));
		objLogCollection.add(String.format("::: Image2D Object Ref. ID: %d%n", 
			textureRefObj.getImage2DObjIndex()));
		objLogCollection.add(String.format("::: Unknown Part #2: %s%n", 
		    HEXUtils.hexToString(textureRefObj.getUnkPart2())));
	}
	
	// 0x15
	private void getObjVertexBufferInfo(M3GObjVertexBuffer vertexBufferObj, List<String> objLogCollection) {
		objLogCollection.add(String.format("::: Color RGBA: %s%n", 
				HEXUtils.hexToString(vertexBufferObj.getColorRGBA())));
		objLogCollection.add(String.format("::: Position Vertex Array Object Ref. ID: %d%n", 
				vertexBufferObj.getPositionVertexArrayObjIndex()));
		objLogCollection.add(String.format("::: Position Bias: [%f, %f, %f]%n", 
				vertexBufferObj.getPositionBias()[0], vertexBufferObj.getPositionBias()[1], 
				vertexBufferObj.getPositionBias()[2]));
		objLogCollection.add(String.format("::: Position Scale: %f%n", 
				vertexBufferObj.getPositionScale()));
		objLogCollection.add(String.format("::: Normals Vertex Array Object Ref. ID: %d%n", 
				vertexBufferObj.getNormalsVertexArrayObjIndex()));
		objLogCollection.add(String.format("::: Colors Object Ref. ID: %d%n", 
				vertexBufferObj.getColorsObjIndex()));
		objLogCollection.add(String.format("::: Texture Coord Array Count: %d%n", 
				vertexBufferObj.getTextureCoordArrayCount()));
		
		int logCounter = 0;
		for (M3GSubObjTextureCoord texCoordSubObj : vertexBufferObj.getTextureCoordArray()) {
			objLogCollection.add(String.format("::: Texture Coord sub #%d, Object Ref. ID: %d%n", 
					logCounter, texCoordSubObj.getTextureCoordObjIndex()));
			objLogCollection.add(String.format("::: Texture Coord sub #%d, Bias: [%f, %f, %f]%n", 
					logCounter, texCoordSubObj.getTextureCoordBias()[0], 
					texCoordSubObj.getTextureCoordBias()[1], texCoordSubObj.getTextureCoordBias()[2]));
			objLogCollection.add(String.format("::: Texture Coord sub #%d, Texture Coord Scale: %f%n", 
					logCounter, texCoordSubObj.getTextureCoordScale()));
			logCounter++;
		}
		
		objLogCollection.add(String.format("::: Tangents Object Ref. ID: %d%n", 
				vertexBufferObj.getTangentsVertexArrayObjIndex()));
		objLogCollection.add(String.format("::: Binormals Object Ref. ID: %d%n", 
				vertexBufferObj.getBinormalsVertexArrayObjIndex()));
	}
	
	// 0x64
	private void getObjSubMeshInfo(M3GObjSubMesh subMeshObj, List<String> objLogCollection) {
		int logCounter = 0;
		objLogCollection.add(String.format("::: Parameter Count: %d%n", 
				subMeshObj.getParameterCount() ));
		for (M3GSubObjParameter subParamLog : subMeshObj.getParameterArray()) {
			getSubObjParameterInfo(subParamLog, objLogCollection, logCounter);
			logCounter++;
		}
		objLogCollection.add(String.format("::: Index Buffer Object Ref. ID: %d%n", 
				subMeshObj.getIndexBufferObjIndex() ));
		objLogCollection.add(String.format("::: Appearance Object Ref. ID: %d%n", 
				subMeshObj.getAppearanceObjIndex() ));
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
