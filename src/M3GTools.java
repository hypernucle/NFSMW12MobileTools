import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Checksum;

import util.HEXClasses.*;
import util.HEXUtils;
import util.LaunchParameters;
import util.LogEntity;
import util.M3GJson;
import util.M3GObjectType;

// M3G information based on "M3G2FBX" tool by RaduMC
public class M3GTools {
	
	private static final byte[] IM2M3G_HEADER = HEXUtils.decodeHexStr("AB494D324D3347BB"); // «IM2M3G»
	private static final byte[] IM2M3G_HEADER_PART2 = HEXUtils.decodeHexStr("0D0A1A0A");
	
	private static final int IM2M3G_HEADER_SIZE = 0xC;
	private static final int IM2M3G_FILESIZESPART_SIZE = 0x9;
	private static final UUID emptyUUID = new UUID(0L, 0L);

	private static int curPos = 0x0;
	private static int curOrderId = 1; // Header counts too
	private static Map<Integer, UUID> orderMap = new HashMap<>();
	private static Map<UUID, Integer> revOrderMap = new HashMap<>();
	
	private static final Logger jl = Logger.getLogger(LogEntity.class.getSimpleName());
	private StringBuilder strLog = new StringBuilder();
	
	public void mapM3G(String[] args) throws IOException {
		String filePath = args[1];
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
		
		while (m3gBytes.length - getCurPos() != 0x4) { // Ending empty part (or possible checksum)
			readNextObject(m3gBytes, m3g);
			nextCurOrderId();
		}
		m3g.setChecksum(getBytesFromCurPos(m3gBytes, 0x4));
		
		jl.log(Level.INFO, "Map log has been saved, object count: " + getCurOrderId() + ".");
		// TODO better file name
		Files.write(Paths.get(filePath + ".map.txt"), strLog.toString().getBytes(StandardCharsets.UTF_8));
		
		M3GJson.outputSBJson(m3g, filePath);
		if (LaunchParameters.isM3GReaderCheck()) {
			checkM3GReaderResult(m3g, m3gBytes, filePath);
		}
	}
	
	public void buildFromJson(String filePath) throws IOException {
		try {
			M3GModel m3g = M3GJson.loadM3GJson(filePath);
			revOrderMap.put(emptyUUID, 0x0); // Null value
			for (M3GObject obj : m3g.getObjectArray()) {
				// Save UUID of the object in order
				revOrderMap.putIfAbsent(obj.getUUID(), getCurOrderId());
				// Save the actual Integer reference IDs
				obj.assignUUIDWithOrderIds(revOrderMap);
				nextCurOrderId();
			}
			
			Files.write(Paths.get(filePath + ".bin"), m3g.toByteArray());
		} catch (NoSuchFileException noFile) {
			jl.log(Level.SEVERE, "File cannot be found ({0}), aborted.", filePath);
		} catch (NullPointerException npe) {
			jl.log(Level.SEVERE, "One of the object #{0} references defined wrong.", getCurOrderId());
			//throw new NullPointerException();
		}
	}
	
	//
	//
	//
	
	private void checkM3GReaderResult(M3GModel m3g, byte[] m3gBytes, String filePath) throws IOException {
		ByteArrayOutputStream newStream = new ByteArrayOutputStream();
		newStream.write(m3g.toByteArray());
		Files.write(Paths.get(filePath + ".new.bin"), m3g.toByteArray());
		
		Checksum origCRC = HEXUtils.getFileBytesChecksum(m3gBytes);
		Checksum newCRC = HEXUtils.getFileBytesChecksum(newStream.toByteArray());
		String result = String.format("Rebuilded .m3g file have a 1:1 byte match: %s", 
				origCRC.getValue() == newCRC.getValue());
		jl.log(Level.INFO, result);
	}
	
	//
	//
	//
	
	private void readNextObject(byte[] m3gBytes, M3GModel m3g) throws IOException {
		M3GObjGeneric obj = new M3GObjGeneric();
		obj.setOrderId(getCurOrderId());
		obj.setStartAddr(getCurPos());
		obj.setType(passByteFromCurPos(m3gBytes));
		obj.setSize(passBytesFromCurPos(m3gBytes, 0x4));
		// Keep the UUID object references instead of direct ID values, for easier edits & re-ordering
		obj.setUUID(UUID.randomUUID());
		orderMap.putIfAbsent(obj.getOrderId(), obj.getUUID());
		
		List<String> objLogCollection = new ArrayList<>();
		M3GObjectType objTypeEnum = M3GObjectType.valueOf(obj.getType());
		m3g.addObject(readObjectProperties(m3gBytes, obj, objTypeEnum, objLogCollection));
		
		int endAddr = getCurPos();
		String objTypeStr = objTypeEnum != null ? objTypeEnum.toString() 
				: "0x" + HEXUtils.byteToHexString(obj.getTypeByte());
		strLog.append(String.format("Object #%d type: %s, size: %d (0x%s - 0x%s)%n", 
				obj.getOrderId(), objTypeStr, obj.getSize(), 
				HEXUtils.hexToString(HEXUtils.intToByteArrayBE(obj.getStartAddr())), 
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
			return readObjAnimationTrack(m3gBytes, objTemp, objLogCollection);
		case APPEARANCE:
			return readObjAppearance(m3gBytes, objTemp, objLogCollection);
		case GROUP:
			return readObjGroup(m3gBytes, objTemp, objLogCollection);
		case CLONED_GROUP:
			return readObjClonedGroup(m3gBytes, objTemp, objLogCollection);
		case IMAGE2D:
			return readObjImage2D(m3gBytes, objTemp, objLogCollection);
		case MESH_CONFIG:
			return readObjMesh(m3gBytes, objTemp, objLogCollection);
		case SUB_MESH:
			return readObjSubMesh(m3gBytes, objTemp, objLogCollection);
		case TEXTURE_REF:
			return readObjTextureRef(m3gBytes, objTemp, objLogCollection);
		case VERTEX_ARRAY:
			return readObjVertexArray(m3gBytes, objTemp, objLogCollection);
		case VERTEX_BUFFER:
			return readObjVertexBuffer(m3gBytes, objTemp, objLogCollection);
		case INDEX_BUFFER:
			return readObjIndexBuffer(m3gBytes, objTemp, objLogCollection);
		case HEADER_ELEMENT: case ANIMATION_TRACK_SETTINGS: case COMPOSITING_MODE: 
		case POLYGON_MODE: case ANIMATION_BUFFER: 
		default:
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
	
	private void copyBasicObjInfo(M3GObject newObj, M3GObjGeneric objTemp) {
		newObj.setOrderId(objTemp.getOrderId());
		newObj.setStartAddr(objTemp.getStartAddr());
		newObj.setType(objTemp.getTypeByte());
		newObj.setSize(objTemp.getSizeBytes());
		newObj.setUUID(objTemp.getUUID());
	}
	
	private UUID getOrderReferenceUUID(int id) {
		UUID refId = orderMap.getOrDefault(id, emptyUUID);
		if (id != 0x0 && refId.equals(emptyUUID)) {
			jl.log(Level.SEVERE, "Object reference #{0} cannot be found! "
					+ "All child objects must be placed before the object in read.", id);
			throw new NullPointerException();
		}
		return refId;
	}
	
	//
	// Object reading classes
	//
	
	// 0x2
	private M3GObjAnimationTrack readObjAnimationTrack(byte[] m3gBytes, 
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjAnimationTrack animationTrackObj = new M3GObjAnimationTrack();
		copyBasicObjInfo(animationTrackObj, objTemp);
		
		changeCurPos(animationTrackObj.getPadding().length);
		//
		animationTrackObj.setAnimationBufferObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		animationTrackObj.setAnimationBufferObjUUID(getOrderReferenceUUID(
				animationTrackObj.getAnimationBufferObjIndex()));
		//
		animationTrackObj.setAnimationTrackSettingsObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		animationTrackObj.setAnimationTrackSettingsObjUUID(getOrderReferenceUUID(
				animationTrackObj.getAnimationTrackSettingsObjIndex()));
		//
		animationTrackObj.setUnkPart(passBytesFromCurPos(m3gBytes, 0x4));
		
		getObjAnimationTrackInfo(animationTrackObj, objLogCollection);
		return animationTrackObj;
	}
	
	// 0x3
	private M3GObjAppearance readObjAppearance(byte[] m3gBytes, 
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjAppearance appearanceObj = new M3GObjAppearance();
		copyBasicObjInfo(appearanceObj, objTemp);
		
		appearanceObj.setAnimationControllers(passBytesFromCurPos(m3gBytes, 0x4));
		appearanceObj.setAnimationTracks(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < appearanceObj.getAnimationTracks(); i++) {
			int orderId = HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4));
			appearanceObj.addToAnimationArray(orderId);
			appearanceObj.addToAnimationArrayObjUUIDArray(getOrderReferenceUUID(orderId));
		}
		appearanceObj.setParameterCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < appearanceObj.getParameterCount(); i++) {
			appearanceObj.addToParameterArray(readSubParameterObject(m3gBytes));
		}
		appearanceObj.setLayer(passByteFromCurPos(m3gBytes));
		//
		appearanceObj.setCompositingModeObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		appearanceObj.setCompositingModeObjUUID(getOrderReferenceUUID(
				appearanceObj.getCompositingModeObjIndex()));
		//
		appearanceObj.setFogObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		appearanceObj.setFogObjUUID(getOrderReferenceUUID(
				appearanceObj.getFogObjIndex()));
		//
		appearanceObj.setPolygonModeObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		appearanceObj.setPolygonModeObjUUID(getOrderReferenceUUID(
				appearanceObj.getPolygonModeObjIndex()));
		//
		appearanceObj.setMaterialObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		appearanceObj.setMaterialObjUUID(getOrderReferenceUUID(
				appearanceObj.getMaterialObjIndex()));
		//
		appearanceObj.setTextureRefCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < appearanceObj.getTextureRefCount(); i++) {
			int orderId = HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4));
			appearanceObj.addToTextureRefObjIndexArray(orderId);
			appearanceObj.addToTextureRefObjUUIDArray(getOrderReferenceUUID(orderId));
		}
		
		getObjAppearanceInfo(appearanceObj, objLogCollection);
		return appearanceObj;
	}
	
	// 0x9
	private M3GObjGroup readObjGroup(byte[] m3gBytes, 
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjGroup groupObj = new M3GObjGroup();
		copyBasicObjInfo(groupObj, objTemp);
		
		fillObjGroupBase(groupObj, m3gBytes);
		
		groupObj.setChildCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < groupObj.getChildCount(); i++) { 
			int orderId = HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4));
			groupObj.addToChildObjIndexArray(orderId);
			groupObj.addToChildObjUUIDArray(getOrderReferenceUUID(orderId));
		}
		
		getObjGroupInfo(groupObj, objLogCollection);
		return groupObj;
	}
	
	private void fillObjGroupBase(M3GObjGroupBase groupBaseObj, byte[] m3gBytes) {
		groupBaseObj.setAnimationControllers(passBytesFromCurPos(m3gBytes, 0x4));
		groupBaseObj.setAnimationTracks(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < groupBaseObj.getAnimationTracks(); i++) {
			int orderId = HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4));
			groupBaseObj.addToAnimationArray(orderId);
			groupBaseObj.addToAnimationArrayObjUUIDArray(getOrderReferenceUUID(orderId));
		}
		groupBaseObj.setParameterCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < groupBaseObj.getParameterCount(); i++) {
			groupBaseObj.addToParameterArray(readSubParameterObject(m3gBytes));
		}
		groupBaseObj.setHasComponentTransform(passByteFromCurPos(m3gBytes));
		if (groupBaseObj.getHasComponentTransform() != 0) {
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
			groupBaseObj.setComponentTransform(compTrans);
		}
		groupBaseObj.setHasGeneralTransform(passByteFromCurPos(m3gBytes)); // TODO
		if (groupBaseObj.getHasGeneralTransform() != 0) {
			groupBaseObj.setGeneralTransformBytes(passBytesFromCurPos(m3gBytes, 0x40));
		}
		groupBaseObj.setUnkPart(passBytesFromCurPos(m3gBytes, 0x7));
		groupBaseObj.setHasUnkFuncPart(passByteFromCurPos(m3gBytes)); // TODO
	}
	
	// 0xA
	private M3GObjImage2D readObjImage2D(byte[] m3gBytes, 
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjImage2D image2DObj = new M3GObjImage2D();
		copyBasicObjInfo(image2DObj, objTemp);
		
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
	private M3GObjMeshConfig readObjMesh(byte[] m3gBytes, 
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjMeshConfig meshObj = new M3GObjMeshConfig();
		copyBasicObjInfo(meshObj, objTemp);
		
		changeCurPos(meshObj.getPadding().length);
		meshObj.setUnkPart(passBytesFromCurPos(m3gBytes, 0xE));
		//
		meshObj.setVertexBufferObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		meshObj.setVertexBufferObjUUID(getOrderReferenceUUID(
				meshObj.getVertexBufferObjIndex()));
		//
		meshObj.setSubMeshCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < meshObj.getSubMeshCount(); i++) {
			int orderId = HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4));
			meshObj.addToSubMeshObjIndexArray(orderId);
			meshObj.addToSubMeshObjUUIDArray(getOrderReferenceUUID(orderId));
		}
		
		getObjMeshInfo(meshObj, objLogCollection);
		return meshObj;
	}
	
	// 0x10
	private M3GObjClonedGroup readObjClonedGroup(byte[] m3gBytes, 
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjClonedGroup clonedGroupObj = new M3GObjClonedGroup();
		copyBasicObjInfo(clonedGroupObj, objTemp);

		fillObjGroupBase(clonedGroupObj, m3gBytes);
		
		clonedGroupObj.setCommonVertexBufferObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		clonedGroupObj.setCommonVertexBufferObjUUID(getOrderReferenceUUID(
				clonedGroupObj.getCommonVertexBufferObjIndex()));
		//
		clonedGroupObj.setObjLODCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < clonedGroupObj.getObjLODCount(); i++) { 
			int orderId = HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4));
			clonedGroupObj.addToObjLODIndexArray(orderId);
			clonedGroupObj.addToObjLODUUIDArray(getOrderReferenceUUID(orderId));
		}
		//
		clonedGroupObj.setGroupObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		clonedGroupObj.setGroupObjUUID(getOrderReferenceUUID(
				clonedGroupObj.getGroupObjIndex()));
		//
		clonedGroupObj.setVertexArray1ObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		clonedGroupObj.setVertexArray1ObjUUID(getOrderReferenceUUID(
				clonedGroupObj.getVertexArray1ObjIndex()));
		//
		clonedGroupObj.setVertexArray2ObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		clonedGroupObj.setVertexArray2ObjUUID(getOrderReferenceUUID(
				clonedGroupObj.getVertexArray2ObjIndex()));
		//
		clonedGroupObj.setMountGroupsObjCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < clonedGroupObj.getMountGroupsObjCount(); i++) { 
			int orderId = HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4));
			clonedGroupObj.addToMountGroupsObjIndexArray(orderId);
			clonedGroupObj.addToMountGroupsObjUUIDArray(getOrderReferenceUUID(orderId));
		}
		
		getObjClonedGroupInfo(clonedGroupObj, objLogCollection);
		return clonedGroupObj;
	}
	
	// 0x11
	private M3GObjTextureRef readObjTextureRef(byte[] m3gBytes, 
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjTextureRef textureRefObj = new M3GObjTextureRef();
		copyBasicObjInfo(textureRefObj, objTemp);
		
		changeCurPos(textureRefObj.getPadding().length);
		textureRefObj.setUnkPart(passBytesFromCurPos(m3gBytes, 0x2));
		//
		textureRefObj.setImage2DObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		textureRefObj.setImage2DObjUUID(getOrderReferenceUUID(
				textureRefObj.getImage2DObjIndex()));
		//
		textureRefObj.setUnkPart2(passBytesFromCurPos(m3gBytes, 0x8));

		getObjTextureRefInfo(textureRefObj, objLogCollection);
		return textureRefObj;
	}
	
	// 0x14
	private M3GObjVertexArray readObjVertexArray(byte[] m3gBytes, 
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjVertexArray vertexArrayObj = new M3GObjVertexArray();
		copyBasicObjInfo(vertexArrayObj, objTemp);
		
		changeCurPos(vertexArrayObj.getPadding().length);
		vertexArrayObj.setComponentSize(passByteFromCurPos(m3gBytes));
		vertexArrayObj.setComponentCount(passByteFromCurPos(m3gBytes));
		vertexArrayObj.setEncoding(passByteFromCurPos(m3gBytes));
		if (vertexArrayObj.getEncoding() != 0x0) {
			jl.log(Level.SEVERE, "One of Vertex Array contains encoded element, various issues may happen.");
		}
		vertexArrayObj.setVertexCount(passBytesFromCurPos(m3gBytes, 0x2));
		
		int elementSize = vertexArrayObj.getComponentSize() * vertexArrayObj.getComponentCount();
		byte[] vertexArrayRaw = passBytesFromCurPos(m3gBytes, elementSize * vertexArrayObj.getVertexCount());
		// reader position passes entire array here
		List<byte[]> vertexArray = HEXUtils.splitByteArray(vertexArrayRaw, elementSize);
		System.out.println(vertexArray.size());
		for (byte[] vertexElement : vertexArray) {
			switch(vertexArrayObj.getComponentSize()) {
			case 0x1:
				if (vertexArrayObj.getComponentCount() == 0x4) { // RGBA
					vertexArrayObj.addToVertexArray(new float[] {
							vertexElement[0], vertexElement[1], // divide all by 255
							vertexElement[2], vertexElement[3]
					});
				} else {
					float[] element = new float[vertexArrayObj.getComponentCount()];
					for (int b = 0; b < vertexArrayObj.getComponentCount(); b++) {
						element[b] = vertexElement[b]; // * positionScale[c] + positionBias[c]
					}
					vertexArrayObj.addToVertexArray(element);
				}
				break;
			case 0x2:
				float[] element = new float[vertexArrayObj.getComponentCount()];
				for (int b = 0; b < vertexArrayObj.getComponentCount(); b++) {
					int int16value = HEXUtils.twoLEByteArrayToInt(Arrays.copyOfRange(vertexElement, 0 + (b * 2), 2 + (b * 2)));
					element[b] = (float)int16value; // * positionScale[c] + positionBias[c]
				}
				vertexArrayObj.addToVertexArray(element);
				break;
			case 0x4:
				float[] element2 = new float[vertexArrayObj.getComponentCount()];
				for (int b = 0; b < vertexArrayObj.getComponentCount(); b++) {
					element2[b] = HEXUtils.bytesToFloat(Arrays.copyOfRange(vertexElement, 0 + (b * 4), 4 + (b * 4))); // * positionScale[c] + positionBias[c]
				}
				vertexArrayObj.addToVertexArray(element2);
				break;
			default: break;
			}
		}
		
		getObjVertexArrayInfo(vertexArrayObj, objLogCollection);
		return vertexArrayObj;
	}
	
	// 0x15
	private M3GObjVertexBuffer readObjVertexBuffer(byte[] m3gBytes,
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjVertexBuffer vertexBufferObj = new M3GObjVertexBuffer();
		copyBasicObjInfo(vertexBufferObj, objTemp);
		
		changeCurPos(vertexBufferObj.getPadding().length);
		vertexBufferObj.setColorRGBA(passBytesFromCurPos(m3gBytes, 0x4));
		//
		vertexBufferObj.setPositionVertexArrayObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		vertexBufferObj.setPositionVertexArrayObjUUID(getOrderReferenceUUID(
				vertexBufferObj.getPositionVertexArrayObjIndex()));
		//
		
		vertexBufferObj.setPositionBias(new float[] {
			HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
			HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
			HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4))
		});
		vertexBufferObj.setPositionScale(passBytesFromCurPos(m3gBytes, 0x4));
		//
		vertexBufferObj.setNormalsVertexArrayObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		vertexBufferObj.setNormalsVertexArrayObjUUID(getOrderReferenceUUID(
				vertexBufferObj.getNormalsVertexArrayObjIndex()));
		//
		vertexBufferObj.setColorsObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		vertexBufferObj.setColorsObjUUID(getOrderReferenceUUID(
				vertexBufferObj.getColorsObjIndex()));
		//
		
		byte[] texCoordArrayCountBytes = passBytesFromCurPos(m3gBytes, 0x4);
		// -1 means 1 here
		int realTexCoordArrayCount = Math.abs(HEXUtils.byteArrayToInt(texCoordArrayCountBytes));
		vertexBufferObj.setTextureCoordArrayCount(texCoordArrayCountBytes);
		
		for (int i = 0; i < realTexCoordArrayCount; i++) {
			M3GSubObjTextureCoord texCoordObj = new M3GSubObjTextureCoord();
			//
			texCoordObj.setTextureCoordObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
			texCoordObj.setTextureCoordObjUUID(getOrderReferenceUUID(
					texCoordObj.getTextureCoordObjIndex()));
			//
			texCoordObj.setTextureCoordBias(new float[] {
				HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)),
				HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4)), // M3G2FBX: 1 - coordBias[1]
				HEXUtils.bytesToFloat(passBytesFromCurPos(m3gBytes, 0x4))
			});
			texCoordObj.setTextureCoordScale(passBytesFromCurPos(m3gBytes, 0x4));
			
			vertexBufferObj.addToTextureCoordArray(texCoordObj);
		}
		vertexBufferObj.setTangentsVertexArrayObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		vertexBufferObj.setTangentsVertexArrayObjUUID(getOrderReferenceUUID(
				vertexBufferObj.getTangentsVertexArrayObjIndex()));
		//
		vertexBufferObj.setBinormalsVertexArrayObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		vertexBufferObj.setBinormalsVertexArrayObjUUID(getOrderReferenceUUID(
				vertexBufferObj.getBinormalsVertexArrayObjIndex()));
		//
		
		getObjVertexBufferInfo(vertexBufferObj, objLogCollection);
		return vertexBufferObj;
	}
	
	// 0x64
	private M3GObjSubMesh readObjSubMesh(byte[] m3gBytes,
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjSubMesh subMeshObj = new M3GObjSubMesh();
		copyBasicObjInfo(subMeshObj, objTemp);
		
		changeCurPos(subMeshObj.getPadding().length);
		subMeshObj.setParameterCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < subMeshObj.getParameterCount(); i++) {
			subMeshObj.addToParameterArray(readSubParameterObject(m3gBytes));
		}
		subMeshObj.setIndexBufferObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		subMeshObj.setIndexBufferObjUUID(getOrderReferenceUUID(
				subMeshObj.getIndexBufferObjIndex()));
		//
		subMeshObj.setAppearanceObjIndex(passBytesFromCurPos(m3gBytes, 0x4));
		subMeshObj.setAppearanceObjUUID(getOrderReferenceUUID(
				subMeshObj.getAppearanceObjIndex()));
		//
		
		getObjSubMeshInfo(subMeshObj, objLogCollection);
		return subMeshObj;
	}
	
	// 0x65
	private M3GObjIndexBuffer readObjIndexBuffer(byte[] m3gBytes, 
			M3GObjGeneric objTemp, List<String> objLogCollection) {
		M3GObjIndexBuffer indexBufferObj = new M3GObjIndexBuffer();
		copyBasicObjInfo(indexBufferObj, objTemp);

		changeCurPos(indexBufferObj.getPadding().length);
		indexBufferObj.setEncoding(passByteFromCurPos(m3gBytes));
		switch(indexBufferObj.getEncoding()) {
		case 0x0: // Start ID as Integer
			indexBufferObj.setStartIndex(passBytesFromCurPos(m3gBytes, 0x4));
			readObjIndexBufferStripId(m3gBytes, indexBufferObj);
			break; 
		case 0x1: // Start ID as Byte
			indexBufferObj.setStartIndexByte(passByteFromCurPos(m3gBytes));
			readObjIndexBufferStripId(m3gBytes, indexBufferObj);
			break; 
		case 0x2: // Start ID as Short
			indexBufferObj.setStartIndexShort(passBytesFromCurPos(m3gBytes, 0x2));
			readObjIndexBufferStripId(m3gBytes, indexBufferObj);
			break; 
		case 0x80: // Integer
			indexBufferObj.setIndexCount(passBytesFromCurPos(m3gBytes, 0x4));
			for (int i = 0; i < indexBufferObj.getIndexCount(); i++) {
				indexBufferObj.addToIndexArray(HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4)));
			}	
			break;
		case 0x81: // Byte
			indexBufferObj.setIndexCount(passBytesFromCurPos(m3gBytes, 0x4));
			for (int i = 0; i < indexBufferObj.getIndexCount(); i++) {
				indexBufferObj.addToIndexArray(Byte.toUnsignedInt(passByteFromCurPos(m3gBytes)));
			}	
			break;
		case 0x82: // Short
			indexBufferObj.setIndexCount(passBytesFromCurPos(m3gBytes, 0x4));
			for (int i = 0; i < indexBufferObj.getIndexCount(); i++) {
				indexBufferObj.addToIndexArray(HEXUtils.twoLEByteArrayToInt(passBytesFromCurPos(m3gBytes, 0x2)));
			}	
			break;
		default:
			jl.log(Level.SEVERE, "Weird Index Buffer #{0} object type!", indexBufferObj.getEncoding());
			break;
		}
		indexBufferObj.setUnkIntValue(passBytesFromCurPos(m3gBytes, 0x4));

		getObjIndexBufferInfo(indexBufferObj, objLogCollection);
		return indexBufferObj;
	}
	
	private void readObjIndexBufferStripId(byte[] m3gBytes, M3GObjIndexBuffer indexBufferObj) {
		indexBufferObj.setStripLengthsCount(passBytesFromCurPos(m3gBytes, 0x4));
		for (int i = 0; i < indexBufferObj.getStripLengthsCount(); i++) {
			indexBufferObj.addToIndexArray(HEXUtils.byteArrayToInt(passBytesFromCurPos(m3gBytes, 0x4)));
		}	
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
		getObjGroupBaseInfo(groupObj, objLogCollection, logCounter);
		
		objLogCollection.add(String.format("::: Child Count: %d%n", 
				groupObj.getChildCount() ));
		for (Integer id : groupObj.getChildObjIndexArray()) {
			objLogCollection.add(String.format("::: Child #%d Object Ref. ID: %d%n", 
					logCounter, id ));
			logCounter++;
		}
	}
	
	private void getObjGroupBaseInfo(M3GObjGroupBase groupBaseObj, List<String> objLogCollection, int logCounter) {
		objLogCollection.add(String.format("::: Animation Controllers: %s%n", 
				HEXUtils.hexToString(groupBaseObj.getAnimationControllers()) ));
		objLogCollection.add(String.format("::: Animation Tracks: %d%n", 
				groupBaseObj.getAnimationTracks() ));
		for (Integer id : groupBaseObj.getAnimationArray()) {
			objLogCollection.add(String.format("::: Animation Track #%d Object Ref. ID: %d%n", 
					logCounter, id ));
			logCounter++;
		}
		logCounter = 0;
		
		objLogCollection.add(String.format("::: Parameter Count: %d%n", 
				groupBaseObj.getParameterCount() ));
		for (M3GSubObjParameter subParamLog : groupBaseObj.getParameterArray()) {
			getSubObjParameterInfo(subParamLog, objLogCollection, logCounter);
			logCounter++;
		}
		logCounter = 0;
		
		objLogCollection.add(String.format("::: Has Component Transform: %d%n", 
				groupBaseObj.getHasComponentTransform() ));
		if (groupBaseObj.getHasComponentTransform() != 0) {
			objLogCollection.add(String.format("::: Component Transform / Translation [%f, %f, %f]%n", 
					groupBaseObj.getComponentTransform().getTranslation()[0],
					groupBaseObj.getComponentTransform().getTranslation()[1],
					groupBaseObj.getComponentTransform().getTranslation()[2] ));
			objLogCollection.add(String.format("::: Component Transform / Scale [%f, %f, %f]%n", 
					groupBaseObj.getComponentTransform().getScale()[0],
					groupBaseObj.getComponentTransform().getScale()[1],
					groupBaseObj.getComponentTransform().getScale()[2] ));
			objLogCollection.add(String.format("::: Component Transform / Orientation Angle: %f%n", 
					groupBaseObj.getComponentTransform().getOrientationAngle() ));
			objLogCollection.add(String.format("::: Component Transform / Orientation Axis X: %f%n", 
					groupBaseObj.getComponentTransform().getOrientationAxisX() ));
			objLogCollection.add(String.format("::: Component Transform / Orientation Axis Y: %f%n", 
					groupBaseObj.getComponentTransform().getOrientationAxisY() ));
			objLogCollection.add(String.format("::: Component Transform / Orientation Axis Z: %f%n", 
					groupBaseObj.getComponentTransform().getOrientationAxisZ() ));
		}
		objLogCollection.add(String.format("::: Has General Transform: %d%n", 
				groupBaseObj.getHasGeneralTransform() ));
		if (groupBaseObj.getHasGeneralTransform() != 0) {
			objLogCollection.add(String.format("::: General Transform Bytes: %s%n", 
					HEXUtils.hexToString(groupBaseObj.getGeneralTransformBytes()) ));
		}
		objLogCollection.add(String.format("::: Unknown Byte Part: %s%n", 
				HEXUtils.hexToString(groupBaseObj.getUnkPart()) ));
		objLogCollection.add(String.format("::: Has Unknown Functional Part: %d%n", 
				groupBaseObj.getHasUnkFuncPart() ));
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
	
	// 0x10
	private void getObjClonedGroupInfo(M3GObjClonedGroup clonedGroupObj, List<String> objLogCollection) {
		int logCounter = 0;
		getObjGroupBaseInfo(clonedGroupObj, objLogCollection, logCounter);
		
		objLogCollection.add(String.format("::: Common Vertex Buffer Object Ref. ID: %d%n", 
				clonedGroupObj.getCommonVertexBufferObjIndex() ));
		
		objLogCollection.add(String.format("::: Object LODs Count: %d%n", 
				clonedGroupObj.getObjLODCount() ));
		for (Integer id : clonedGroupObj.getObjLODIndexArray()) {
			objLogCollection.add(String.format("::: Object LODs #%d Object Ref. ID: %d%n", 
					logCounter, id ));
			logCounter++;
		}
		logCounter = 0;
		
		objLogCollection.add(String.format("::: Group Object Ref. ID: %d%n", 
				clonedGroupObj.getGroupObjIndex() ));
		objLogCollection.add(String.format("::: Vertex Array #1 Object Ref. ID: %d%n", 
				clonedGroupObj.getVertexArray1ObjIndex() ));
		objLogCollection.add(String.format("::: Vertex Array #2 Object Ref. ID: %d%n", 
				clonedGroupObj.getVertexArray2ObjIndex() ));
		
		objLogCollection.add(String.format("::: Mount Groups Count: %d%n", 
				clonedGroupObj.getMountGroupsObjCount() ));
		for (Integer id : clonedGroupObj.getMountGroupsObjIndexArray()) {
			objLogCollection.add(String.format("::: Mount Group #%d Object Ref. ID: %d%n", 
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
	
	// 0x14
	private void getObjVertexArrayInfo(M3GObjVertexArray vertexArrayObj, List<String> objLogCollection) {
		if (!LaunchParameters.isOutputVertexAndIndexes()) {return;}
		objLogCollection.add(String.format("::: Component Size: %d%n", 
				vertexArrayObj.getComponentSize()));
		objLogCollection.add(String.format("::: Component Count: %d%n", 
				vertexArrayObj.getComponentCount()));
		objLogCollection.add(String.format("::: Encoding: %d%n", 
				vertexArrayObj.getEncoding()));
		objLogCollection.add(String.format("::: Vertex Count: %d%n", 
				vertexArrayObj.getVertexCount()));
		
		int logCounter = 0;
		for (float[] floatArray : vertexArrayObj.getVertexArray()) {
			String vertexObj = String.format("::: Vertex #%d [", logCounter);
			int i = 1;
			for (float value : floatArray) {
				vertexObj = vertexObj.concat(getStrVertexArrayFloat(vertexArrayObj, value));
				if (i < vertexArrayObj.getComponentCount()) {
					vertexObj = vertexObj.concat(", ");
				}
				i++;
			}
			vertexObj = String.format("%s]%n", vertexObj);
			objLogCollection.add(vertexObj);
			logCounter++;
		}
	}
	
	private String getStrVertexArrayFloat(M3GObjVertexArray vertexArrayObj, float value) {
		if (vertexArrayObj.getComponentSize() == 0x1 && 
				vertexArrayObj.getComponentCount() == 0x4) {
			return String.valueOf(value / 255);
		}
		return String.valueOf(value);
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
	
	// 0x65
	private void getObjIndexBufferInfo(M3GObjIndexBuffer indexBufferObj, List<String> objLogCollection) {
		if (!LaunchParameters.isOutputVertexAndIndexes()) {return;}
		objLogCollection.add(String.format("::: Encoding: %d%n", 
				indexBufferObj.getEncoding()));
		
		switch(indexBufferObj.getEncoding()) {
		case 0x0: case 0x1: case 0x2:
			objLogCollection.add(String.format("::: Start Index: %d%n", 
					indexBufferObj.getStartIndex()));
			objLogCollection.add(String.format("::: Strip Lengths Count: %d%n", 
					indexBufferObj.getStripLengthsCount()));
			break; 
		case 0x80: case 0x81: case 0x82:
			objLogCollection.add(String.format("::: Index Count: %d%n", 
					indexBufferObj.getIndexCount()));
			break;
		default: break;
		}
		String indexObj = "::: Index Array [";
		int i = 1;
		for (int index : indexBufferObj.getIndexArray()) {
			indexObj = indexObj.concat(String.valueOf(index));		
			if (i < indexBufferObj.getIndexCount()) {
				indexObj = indexObj.concat(", ");
			}
			i++;
		}
		indexObj = String.format("%s]%n", indexObj);
		objLogCollection.add(indexObj);

		objLogCollection.add(String.format("::: Unknown ending value: %d%n", 
				indexBufferObj.getUnkIntValue()));
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
	
	public static int getCurOrderId() {
		return curOrderId;
	}
	private static void nextCurOrderId() {
		curOrderId++;
	}
}
