package util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import jogl.DDSImage;
import jogl.DDSImage.ImageInfo;
import util.DataClasses.SBinDataElement;
import util.DataClasses.SBinJson;
import util.HEXClasses.SBinBlockObj;

public class TextureUtils {
	private TextureUtils() {}
	
	private static final String PARAM_WIDTH = "width";
	private static final String PARAM_HEIGHT = "height";
	private static final String PARAM_DATA = "data";
	private static final String PARAM_FORMAT = "format";
	private static final String PARAM_TEXTURE = "Texture";
	private static final String PARAM_MIPMAPS = "mipmaps";
	
	private static int imageFormatId = DDSImage.D3DFMT_A8R8G8B8;
	
	public static void extractImage(SBinJson sbinJson, byte[] bulkBlock, byte[] bargBlock) throws IOException {
		List<SBinDataElement> textures = DataUtils.getAllDataElementsByStructName(sbinJson, PARAM_TEXTURE);
		List<byte[]> mmOffsets = HEXUtils.splitByteArray(bulkBlock, 0x8);
		int i = 0;
		for (SBinDataElement texParams : textures) {
			int width = Integer.parseInt(DataUtils.getDataFieldByName(texParams, PARAM_WIDTH).getValue());
			int height = Integer.parseInt(DataUtils.getDataFieldByName(texParams, PARAM_HEIGHT).getValue());
			
			// MipMap map comes after the Texture object
			List<String> mipmapsList = getDATAMipmapsList(sbinJson, texParams);
			ByteBuffer[] mipMaps = new ByteBuffer[mipmapsList.size()];
			boolean imageFormatChecked = false;
			
			int curMipmap = 0;
			for (String dataMMId : mipmapsList) {
				int dataId = HEXUtils.strHexToInt(dataMMId);
				SBinDataElement mmLevel = sbinJson.getDataElements().get(dataId);
				if (!mmLevel.getStructName().contentEquals("Image")) {
					System.out.println("!!! Mipmap level object structure is wrong or broken, DATA Id: " + dataId + ".");
				}
				int mmLevelWidth = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, PARAM_WIDTH).getValue());
				int mmLevelHeight = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, PARAM_HEIGHT).getValue());
				
				int bulkElementId = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, PARAM_DATA).getValue());
				if (!imageFormatChecked) { // All Mipmap objects repeats the image format
					String imageFormat = DataUtils.getDataFieldByName(mmLevel, "format").getValue();
					imageFormatChecked = checkSBAImageFormat(imageFormat);
					if (!imageFormatChecked) {
						System.out.println("!!! This Image format is not supported for unpack or repack (" + imageFormat + "). "
								+ "File contents will be unpacked without the Image.");
						return;
					}
				}
				
				int mmLevelStartOffset = HEXUtils.byteArrayToInt(
						Arrays.copyOfRange(mmOffsets.get(bulkElementId), 0, 4));
				int mmLevelAdditionOffset = HEXUtils.byteArrayToInt(
						Arrays.copyOfRange(mmOffsets.get(bulkElementId), 4, 8));
				byte[] mmData = Arrays.copyOfRange(
						bargBlock, mmLevelStartOffset, mmLevelStartOffset + mmLevelAdditionOffset);
				mipMaps[curMipmap] = ByteBuffer.wrap(
						processMipmapImageOperations(mmData, mmLevelWidth, mmLevelHeight, true));
				curMipmap++;
			}
			DDSImage image = DDSImage.createFromData(imageFormatId, width, height, mipMaps);
			String nameAddition = textures.size() > 1 ? "_" + i : "";
			image.write(new File(sbinJson.getFileName() + nameAddition + ".dds"));
			i++;
		}
	}
	
	public static void repackImage(SBinBlockObj block, SBinJson sbinJson) throws IOException {
		List<SBinDataElement> textures = DataUtils.getAllDataElementsByStructName(sbinJson, PARAM_TEXTURE);
		ByteArrayOutputStream bulkMapStream = new ByteArrayOutputStream();
		ByteArrayOutputStream imageHexStream = new ByteArrayOutputStream();
		int bulkStartOffset = 0;
		
		int i = 0;
		for (SBinDataElement texParams : textures) {
			int width = Integer.parseInt(DataUtils.getDataFieldByName(texParams, PARAM_WIDTH).getValue());
			int height = Integer.parseInt(DataUtils.getDataFieldByName(texParams, PARAM_HEIGHT).getValue());
			SBinDataElement mmmap = DataUtils.getDataElementFromValueId(sbinJson, texParams, PARAM_MIPMAPS);
			
			String nameAddition = textures.size() > 1 ? "_" + i : "";
			DDSImage image = DDSImage.read(new File(sbinJson.getFileName() + nameAddition + ".dds"));
			setCurrentImageFormat(image.getPixelFormat());
			if (image.getWidth() != width || image.getHeight() != height) {
				System.out.println("!!! Image width, height or format is not compatible with the SBin data - expect broken Output file.");
			}
			
			int curMipmap = 0;
			for (String dataMMId : mmmap.getMapElements()) {
				int dataId = HEXUtils.strHexToInt(dataMMId);
				SBinDataElement mmLevel = sbinJson.getDataElements().get(dataId);
				int mmLevelWidth = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, PARAM_WIDTH).getValue());
				int mmLevelHeight = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, PARAM_HEIGHT).getValue());
				String format = DataUtils.getDataFieldByName(mmLevel, PARAM_FORMAT).getValue();
				
				if (!checkDDSImageFormat(format)) {
					System.out.println("!!! DDS Image format (" + format + ") is not equal with the SBin data - expect broken Output file.");
				}
				ImageInfo mipmapLevelInfo = image.getAllMipMaps()[curMipmap];
				if (mmLevelWidth != mipmapLevelInfo.getWidth() || mmLevelHeight != mipmapLevelInfo.getHeight()) {
					System.out.println("!!! Image width or height is not equal to SBin Mipmap #" + curMipmap + " data - expect broken Output file.");
				}
				byte[] mipmapBytes = new byte[mipmapLevelInfo.getData().remaining()];
				mipmapLevelInfo.getData().get(mipmapBytes);
				imageHexStream.write(
						processMipmapImageOperations(mipmapBytes, mmLevelWidth, mmLevelHeight, false));
				
				bulkMapStream.write(HEXUtils.intToByteArrayLE(bulkStartOffset, 0x4));
				bulkStartOffset += mipmapBytes.length;
				int offsetAddition = bulkStartOffset - (bulkStartOffset - mipmapBytes.length);
				bulkMapStream.write(HEXUtils.intToByteArrayLE(offsetAddition, 0x4));
				curMipmap++;
			}
			i++;
		}
		block.setBULKMap(bulkMapStream.toByteArray());
		block.setBlockBytes(imageHexStream.toByteArray());
	}
	
	//
	
	private static List<String> getDATAMipmapsList(SBinJson sbinJson, SBinDataElement texParams) {
		SBinDataElement mmmap = DataUtils.getDataElementFromValueId(sbinJson, texParams, PARAM_MIPMAPS);
		return LaunchParameters.isMipmapUnpackDisabled() ? mmmap.getMapElements().subList(0, 1) : mmmap.getMapElements();
	}
	
	private static byte[] processMipmapImageOperations(
			byte[] mmData, int mmLevelWidth, int mmLevelHeight, boolean unpack) throws IOException {
		if (unpack) {
			return flipPixelsVertically(interleaveHexImage(mmData), mmLevelWidth, mmLevelHeight);
		} else {
			return interleaveHexImage(flipPixelsVertically(mmData, mmLevelWidth, mmLevelHeight));
		}
	}
	
	private static byte[] interleaveHexImage(byte[] imageHex) throws IOException {
		ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
		
		if (imageFormatId == DDSImage.D3DFMT_A8R8G8B8) {
			for (byte[] pixel : HEXUtils.splitByteArray(imageHex, 0x4)) {
				imageStream.write(new byte[]{
						pixel[2], pixel[1], pixel[0], pixel[3]
				});
			}
		} else if (imageFormatId == DDSImage.D3DFMT_R8G8B8) {
			for (byte[] pixel : HEXUtils.splitByteArray(imageHex, 0x3)) {
				imageStream.write(new byte[]{
						pixel[2], pixel[1], pixel[0]
				});
			}
		}
		return imageStream.toByteArray();
	}
	
	// https://stackoverflow.com/q/53660805
	private static byte[] flipPixelsVertically(byte[] imageHex, int width, int height) {
		int pixelSize = imageFormatId == DDSImage.D3DFMT_A8R8G8B8 ? 4 : 3;
		
		byte[] data = new byte[imageHex.length];
        for (int k = 0, j = height - 1; j >= 0 && k < height; j--, k++)
        {
            for (int i = 0; i < width * pixelSize; i++)
            {
            	data[k * width * pixelSize + i] = imageHex[j * width * pixelSize + i];
            }
        }
        return data;
    }
	
	private static boolean checkSBAImageFormat(String imageFormat) {
		boolean isCompatibleFormat = false;
		if (imageFormat.contentEquals("RGBA")) {
			imageFormatId = DDSImage.D3DFMT_A8R8G8B8;
			isCompatibleFormat = true;
		} else if (imageFormat.contentEquals("RGB")) {
			imageFormatId = DDSImage.D3DFMT_R8G8B8;
			isCompatibleFormat = true;
		}
		return isCompatibleFormat;
	}
	
	private static void setCurrentImageFormat(int imageFormat) {
		imageFormatId = imageFormat;
	}
	
	private static boolean checkDDSImageFormat(String dataFormatInfo) {
		boolean isCompatibleWithSBA = false;
		if ( (imageFormatId == DDSImage.D3DFMT_A8R8G8B8 && dataFormatInfo.contentEquals("RGBA"))
				|| (imageFormatId == DDSImage.D3DFMT_R8G8B8 && dataFormatInfo.contentEquals("RGB")) ) {
			isCompatibleWithSBA = true;
		}
		return isCompatibleWithSBA;
	}
}
