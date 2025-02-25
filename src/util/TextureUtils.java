package util;

import java.awt.image.BufferedImage;
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
	
	public static void extractImage(SBinJson sbinJson, byte[] bulkBlock, byte[] bargBlock) throws IOException {
		List<SBinDataElement> textures = DataUtils.getAllDataElementsByStructName(sbinJson, "Texture");
		List<byte[]> mmOffsets = HEXUtils.splitByteArray(bulkBlock, 0x8);
		int i = 0;
		for (SBinDataElement texParams : textures) {
			int width = Integer.parseInt(DataUtils.getDataFieldByName(texParams, "width").getValue());
			int height = Integer.parseInt(DataUtils.getDataFieldByName(texParams, "height").getValue());
			
			// MipMap map comes after the Texture object
			SBinDataElement mmmap = DataUtils.getDataElementFromValueId(sbinJson, texParams, "mipmaps");
			int mipmapsCount = mmmap.getMapElements().size();
			ByteBuffer[] mipMaps = new ByteBuffer[mipmapsCount];
			boolean imageFormatChecked = false;
			
			int curMipmap = 0;
			for (String dataMMId : mmmap.getMapElements()) {
				int dataId = HEXUtils.strHexToInt(dataMMId);
				SBinDataElement mmLevel = sbinJson.getDataElements().get(dataId);
				if (!mmLevel.getStructName().contentEquals("Image")) {
					System.out.println("!!! Mipmap level object structure is wrong or broken, DATA Id: " + dataId + ".");
				}
				int mmLevelWidth = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, "width").getValue());
				int mmLevelHeight = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, "height").getValue());
				
				int bulkElementId = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, "data").getValue());
				if (!imageFormatChecked) { // All Mipmap objects repeats the image format
					String imageFormat = DataUtils.getDataFieldByName(mmLevel, "format").getValue();
					imageFormatChecked = checkImageFormat(imageFormat);
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
			DDSImage image = DDSImage.createFromData(DDSImage.D3DFMT_A8R8G8B8, width, height, mipMaps);
			String nameAddition = textures.size() > 1 ? "_" + i : "";
			image.write(new File(sbinJson.getFileName() + nameAddition + ".dds"));
			i++;
		}
	}
	
	public static void repackImage(SBinBlockObj block, SBinJson sbinJson) throws IOException {
		List<SBinDataElement> textures = DataUtils.getAllDataElementsByStructName(sbinJson, "Texture");
		ByteArrayOutputStream bulkMapStream = new ByteArrayOutputStream();
		ByteArrayOutputStream imageHexStream = new ByteArrayOutputStream();
		int bulkStartOffset = 0;
		
		int i = 0;
		for (SBinDataElement texParams : textures) {
			int width = Integer.parseInt(DataUtils.getDataFieldByName(texParams, "width").getValue());
			int height = Integer.parseInt(DataUtils.getDataFieldByName(texParams, "height").getValue());
			SBinDataElement mmmap = DataUtils.getDataElementFromValueId(sbinJson, texParams, "mipmaps");
			
			String nameAddition = textures.size() > 1 ? "_" + i : "";
			DDSImage image = DDSImage.read(new File(sbinJson.getFileName() + nameAddition + ".dds"));
			if (image.getWidth() != width || image.getHeight() != height 
					|| image.getPixelFormat() != DDSImage.D3DFMT_A8R8G8B8) {
				System.out.println("!!! Image width, height or format is not compatible with the SBin data - expect broken Output file.");
			}
			
			int curMipmap = 0;
			for (String dataMMId : mmmap.getMapElements()) {
				int dataId = HEXUtils.strHexToInt(dataMMId);
				SBinDataElement mmLevel = sbinJson.getDataElements().get(dataId);
				int mmLevelWidth = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, "width").getValue());
				int mmLevelHeight = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, "height").getValue());
				
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
		for (byte[] pixel : HEXUtils.splitByteArray(imageHex, 0x4)) {
			imageStream.write(new byte[]{
					pixel[2], pixel[1], pixel[0], pixel[3]
			});
		}
		return imageStream.toByteArray();
	}
	
	// https://stackoverflow.com/q/53660805
	private static byte[] flipPixelsVertically(byte[] imageHex, int width, int height) {
		byte[] data = new byte[imageHex.length];
        for (int k = 0, j = height - 1; j >= 0 && k < height; j--, k++)
        {
            for (int i = 0; i < width * 4; i++)
            {
            	data[k * width * 4 + i] = imageHex[j * width * 4 + i];
            }
        }
        return data;
    }
	
	private static boolean checkImageFormat(String imageFormat) {
		return imageFormat.contentEquals("RGBA");
	}
}
