package util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import jogl.DDSImage;
import jogl.DDSImage.ImageInfo;
import util.DataClasses.SBinDataElement;
import util.HEXClasses.ETC1PKMTexture;
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
	private static SBinTextureFormat curTexFormat;
	
	public static void extractImage(SBinBlockObj bulkBlock, byte[] bargBlock) throws IOException {
		List<SBinDataElement> textures = DataUtils.getAllDataElementsByStructName(PARAM_TEXTURE);
		int i = 0;
		for (SBinDataElement texParams : textures) {
			int width = Integer.parseInt(DataUtils.getDataFieldByName(texParams, PARAM_WIDTH).getValue());
			int height = Integer.parseInt(DataUtils.getDataFieldByName(texParams, PARAM_HEIGHT).getValue());
			
			if (!checkSBAImageFormat(texParams)) { // All Mipmap objects repeats the image format
				System.out.println("!!! This Image format is not supported for unpack or repack (" + curTexFormat.toString() + "). "
						+ "File contents will be unpacked without the Image.");
				return;
			}
			
			// MipMap map comes after the Texture object
			List<String> mipmapsList = processDATAMipmapsList(texParams);
			ByteBuffer[] mipMaps = new ByteBuffer[mipmapsList.size()];
			
			int curMipmap = 0;
			for (String dataMMId : mipmapsList) {
				int dataId = HEXUtils.strHexToInt(dataMMId);
				SBinDataElement mmLevel = SBJson.get().getDataElements().get(dataId);
				if (!mmLevel.getStructName().contentEquals("Image")) {
					System.out.println("!!! Mipmap level object structure is wrong or broken, DATA Id: " + dataId + ".");
				}
				int mmLevelWidth = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, PARAM_WIDTH).getValue());
				int mmLevelHeight = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, PARAM_HEIGHT).getValue());
				int bulkElementId = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, PARAM_DATA).getValue());
				
				int mmLevelStartOffset = HEXUtils.byteArrayToInt(
						Arrays.copyOfRange(bulkBlock.getBlockElements().get(bulkElementId), 0, 4));
				int mmLevelAdditionOffset = HEXUtils.byteArrayToInt(
						Arrays.copyOfRange(bulkBlock.getBlockElements().get(bulkElementId), 4, 8));
				byte[] mmData = Arrays.copyOfRange(
						bargBlock, mmLevelStartOffset, mmLevelStartOffset + mmLevelAdditionOffset);
				mipMaps[curMipmap] = ByteBuffer.wrap(
						processMipmapImageOperations(mmData, mmLevelWidth, mmLevelHeight, true));
				curMipmap++;
			}
			writeImage(textures.size(), i, width, height, mipMaps);
			i++;
		}
	}
	
	private static void writeImage(int texturesCount, int i, int width, int height, ByteBuffer[] mipMaps) throws IOException {
		String fileName = SBJson.get().getFileName() + (texturesCount > 1 ? "_" + i : "");
		// Assuming we have "etc1tool" from Android SDK
		if (curTexFormat.equals(SBinTextureFormat.ETC_RGB)) {
			ETC1PKMTexture etc1 = new ETC1PKMTexture();
			etc1.setEncWidth(HEXUtils.shortToBytesBE(width));
			etc1.setWidth(HEXUtils.shortToBytesBE(width));
			etc1.setEncHeight(HEXUtils.shortToBytesBE(height));
			etc1.setHeight(HEXUtils.shortToBytesBE(height));
			
			byte[] mipmapBytes = new byte[mipMaps[0].remaining()];
			mipMaps[0].get(mipmapBytes);
			etc1.setImageData(mipmapBytes);
			
			Files.write(Paths.get("tools/etc1tex.bin"), etc1.toByteArray(), 
					StandardOpenOption.WRITE, StandardOpenOption.CREATE, 
					StandardOpenOption.TRUNCATE_EXISTING);
			ProcessBuilder p = new ProcessBuilder(
					"tools/etc1tool.exe", "tools/etc1tex.bin", "--decode", "-o", fileName + ".png");
			p.start();
		} else {
			DDSImage image = DDSImage.createFromData(imageFormatId, width, height, mipMaps);
			image.write(new File(fileName + ".dds"));
		}
	}
	
	public static void repackImage(SBinBlockObj block) throws IOException {
		List<SBinDataElement> textures = DataUtils.getAllDataElementsByStructName(PARAM_TEXTURE);
		ByteArrayOutputStream bulkMapStream = new ByteArrayOutputStream();
		ByteArrayOutputStream imageHexStream = new ByteArrayOutputStream();
		int bulkStartOffset = 0;
		
		int i = 0;
		for (SBinDataElement texParams : textures) {
			int width = Integer.parseInt(DataUtils.getDataFieldByName(texParams, PARAM_WIDTH).getValue());
			int height = Integer.parseInt(DataUtils.getDataFieldByName(texParams, PARAM_HEIGHT).getValue());
			SBinDataElement mmmap = DataUtils.getDataElementFromValueId(texParams, PARAM_MIPMAPS);
			
			String nameAddition = textures.size() > 1 ? "_" + i : "";
			DDSImage image = DDSImage.read(new File(SBJson.get().getFileName() + nameAddition + ".dds"));
			setCurrentImageFormat(image.getPixelFormat());
			if (image.getWidth() != width || image.getHeight() != height) {
				System.out.println("!!! Image width, height or format is not compatible with the SBin data - expect broken Output file.");
			}
			
			int curMipmap = 0;
			for (String dataMMId : mmmap.getMapElements()) {
				int dataId = HEXUtils.strHexToInt(dataMMId);
				SBinDataElement mmLevel = SBJson.get().getDataElements().get(dataId);
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
				
				bulkMapStream.write(HEXUtils.intToByteArrayLE(bulkStartOffset));
				bulkStartOffset += mipmapBytes.length;
				int offsetAddition = bulkStartOffset - (bulkStartOffset - mipmapBytes.length);
				bulkMapStream.write(HEXUtils.intToByteArrayLE(offsetAddition));
				curMipmap++;
			}
			i++;
		}
		block.setBULKMap(bulkMapStream.toByteArray());
		block.setBlockBytes(imageHexStream.toByteArray());
	}
	
	//
	
	private static List<String> processDATAMipmapsList(SBinDataElement texParams) {
		SBinDataElement mmmap = DataUtils.getDataElementFromValueId(texParams, PARAM_MIPMAPS);
		
		return LaunchParameters.isMipmapUnpackDisabled() 
				|| curTexFormat.equals(SBinTextureFormat.ETC_RGB)
				? mmmap.getMapElements().subList(0, 1) : mmmap.getMapElements();
	}
	
	private static byte[] processMipmapImageOperations(
			byte[] mmData, int mmLevelWidth, int mmLevelHeight, boolean unpack) throws IOException {
		if (curTexFormat.equals(SBinTextureFormat.ETC_RGB)) {
			return mmData; // Do nothing
		}
		else if (unpack) {
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
	
	private static boolean checkSBAImageFormat(SBinDataElement texParams) {
		SBinDataElement mmmap = DataUtils.getDataElementFromValueId(texParams, PARAM_MIPMAPS);
		int dataId = HEXUtils.strHexToInt(mmmap.getMapElements().get(0));
		SBinDataElement mmLevel = SBJson.get().getDataElements().get(dataId);
		String imageFormat = DataUtils.getDataFieldByName(mmLevel, "format").getValue();
		
		curTexFormat = SBinTextureFormat.valueOf(imageFormat);
		boolean isSupported = false;
		switch(SBinTextureFormat.valueOf(imageFormat)) {
		case RGB:
			imageFormatId = DDSImage.D3DFMT_R8G8B8;
			isSupported = true;
			break;
		case RGBA:
			imageFormatId = DDSImage.D3DFMT_A8R8G8B8;
			isSupported = true;
			break;
		case ETC_RGB:
			isSupported = true;
			break;
		default: break;
		}
		return isSupported;
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
