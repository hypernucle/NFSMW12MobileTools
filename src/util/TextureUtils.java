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
	
	public static void extractImage(SBinJson sbinJson, byte[] bulkBlock, byte[] bargBlock) throws IOException {
		SBinDataElement texParams = DataUtils.getDataElementByStructName(sbinJson, "Texture");
		int width = Integer.parseInt(DataUtils.getDataFieldByName(texParams, "width").getValue());
		int height = Integer.parseInt(DataUtils.getDataFieldByName(texParams, "height").getValue());
		// MipMap map comes after the Texture object
		SBinDataElement mmmap = DataUtils.getDataElementFromValueId(sbinJson, texParams, "mipmaps");
		
		List<byte[]> mmOffsets = HEXUtils.splitByteArray(bulkBlock, 0x8);
		int mipmapsCount = mmmap.getMapElements().size();
		ByteBuffer[] mipMaps = new ByteBuffer[mipmapsCount];
		boolean imageFormatChecked = false;
		
		for (String dataMMId : mmmap.getMapElements()) {
			int dataId = HEXUtils.strHexToInt(dataMMId);
			SBinDataElement mmLevel = sbinJson.getDataElements().get(dataId);
			if (!mmLevel.getStructName().contentEquals("Image")) {
				System.out.println("!!! Mipmap level object structure is wrong or broken, DATA Id: " + dataId + ".");
			}
			int mmLevelId = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, "data").getValue());
			if (!imageFormatChecked) { // All Mipmap objects repeats the image format
				String imageFormat = DataUtils.getDataFieldByName(mmLevel, "format").getValue();
				imageFormatChecked = checkImageFormat(imageFormat);
				if (!imageFormatChecked) {
					System.out.println("!!! This Image format is not supported for unpack or repack (" + imageFormat + "). "
							+ "File contents will be unpacked without the Image.");
					return;
				}
			}
//			int mmLevelWidth = Integer.parseInt(getDataFieldByName(texParams, "width").getValue());
//			int mmLevelHeight = Integer.parseInt(getDataFieldByName(texParams, "height").getValue());
			
			int mmLevelStartOffset = HEXUtils.byteArrayToInt(
					Arrays.copyOfRange(mmOffsets.get(mmLevelId), 0, 4));
			int mmLevelAdditionOffset = HEXUtils.byteArrayToInt(
					Arrays.copyOfRange(mmOffsets.get(mmLevelId), 4, 8));
			byte[] mmData = interleaveHexImage(Arrays.copyOfRange(
					bargBlock, mmLevelStartOffset, mmLevelStartOffset + mmLevelAdditionOffset));
			mipMaps[mmLevelId] = ByteBuffer.wrap(mmData);
		}
		DDSImage image = DDSImage.createFromData(DDSImage.D3DFMT_A8R8G8B8, width, height, mipMaps);
		image.write(new File(sbinJson.getFileName() + ".dds"));
	}
	
	public static void repackImage(SBinBlockObj block, SBinJson sbinJson) throws IOException {
		SBinDataElement texParams = DataUtils.getDataElementByStructName(sbinJson, "Texture");
		int width = Integer.parseInt(DataUtils.getDataFieldByName(texParams, "width").getValue());
		int height = Integer.parseInt(DataUtils.getDataFieldByName(texParams, "height").getValue());
		SBinDataElement mmmap = DataUtils.getDataElementFromValueId(sbinJson, texParams, "mipmaps");
		
		DDSImage image = DDSImage.read(new File(sbinJson.getFileName() + ".dds"));
		if (image.getWidth() != width || image.getHeight() != height 
				|| image.getPixelFormat() != DDSImage.D3DFMT_A8R8G8B8) {
			System.out.println("!!! Image width, height or format is not compatible with the SBin data - expect broken Output file.");
		}
		if (image.getNumMipMaps() + 1 != mmmap.getMapElements().size()) {
			System.out.println("!!! Image Mipmaps amount is not the same as in SBin data - expect broken Output file.");
		}
		ByteArrayOutputStream imageHexStream = new ByteArrayOutputStream();
		ByteArrayOutputStream bulkMapStream = new ByteArrayOutputStream();
		int startOffset = 0;
		
		for (ImageInfo mipmapLevelInfo : image.getAllMipMaps()) {
			byte[] mipmapBytes = new byte[mipmapLevelInfo.getData().remaining()];
			mipmapLevelInfo.getData().get(mipmapBytes);
			imageHexStream.write(mipmapBytes);
			
			bulkMapStream.write(HEXUtils.intToByteArrayLE(startOffset, 0x4));
			startOffset += mipmapBytes.length;
			int offsetAddition = startOffset - (startOffset - mipmapBytes.length);
			bulkMapStream.write(HEXUtils.intToByteArrayLE(offsetAddition, 0x4));
		}
		block.setBULKMap(bulkMapStream.toByteArray());
		block.setBlockBytes(interleaveHexImage(imageHexStream.toByteArray()));
	}
	
	//
	
	private static byte[] interleaveHexImage(byte[] imageHex) throws IOException {
		ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
		for (byte[] pixel : HEXUtils.splitByteArray(imageHex, 0x4)) {
			imageStream.write(new byte[]{
					pixel[2], pixel[1], pixel[0], pixel[3]
			});
		}
		return imageStream.toByteArray();
	}
	
	private static boolean checkImageFormat(String imageFormat) {
		return imageFormat.contentEquals("RGBA");
	}
}
