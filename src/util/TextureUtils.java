package util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import jogl.DDSImage;
import jogl.DDSImage.ImageInfo;
import util.DataClasses.SBinDataElement;
import util.DataClasses.SBinDataField;
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
	private static final String ETC1_TEMP_PATH = "tools/etc1tex.bin";
	
	private static final String FILE_DDS = ".dds";
	private static final String FILE_PNG = ".png";
	private static final String FMT_R8G8B8 = "R8G8B8";
	private static final String FMT_A8R8G8B8 = "A8R8G8B8";
	
	private static int imageFormatId = DDSImage.D3DFMT_A8R8G8B8;
	private static String formatName = "!pls fix!";
	private static SBinTextureFormat curTexFormat;
	private static SBinTextureFormat prevTexFormat;
	
	public static void extractImage(SBinBlockObj bulkBlock, byte[] bargBlock) throws IOException, InterruptedException {
		List<SBinDataElement> textures = DataUtils.getAllDataElementsByStructName(PARAM_TEXTURE);
		int i = 0;
		for (SBinDataElement texParams : textures) {
			int width = Integer.parseInt(DataUtils.getDataFieldByName(texParams, PARAM_WIDTH).getValue());
			int height = Integer.parseInt(DataUtils.getDataFieldByName(texParams, PARAM_HEIGHT).getValue());
			
			if (!checkSBAImageFormat(texParams)) { // All Mipmap objects repeats the image format
				System.out.println("!!! This Image format is not supported for unpack (" + curTexFormat.toString() + "). "
						+ "File contents will be unpacked without the Image.");
				return;
			}
			
			// MipMap map comes after the Texture object
			SBinDataElement mmmap = DataUtils.getDataElementFromValueId(texParams, PARAM_MIPMAPS);
			List<String> mipmapsList = processDATAMipmapsList(texParams, mmmap);
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
			writeImage(textures.size(), i, width, height, mipMaps, mmmap.getMapElements().size());
			i++;
		}
	}
	
	private static void writeImage(int texturesCount, int i, int width, int height, 
			ByteBuffer[] mipMaps, int mipmapsOriginalCount) throws IOException, InterruptedException {
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
			
			Files.write(Paths.get(ETC1_TEMP_PATH), etc1.toByteArray(), StandardOpenOption.WRITE, 
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			Process etc1Tool = new ProcessBuilder(
					"tools/etc1tool.exe", ETC1_TEMP_PATH, "--decode", "-o", fileName + FILE_PNG).start();
			etc1Tool.waitFor();
			
			Files.deleteIfExists(Paths.get(ETC1_TEMP_PATH)); // Temporary file
			fileName = fileName + FILE_PNG;
			File pngFile = new File(fileName);
			ImageIO.write(flipPNGPixelsVertically(ImageIO.read(pngFile)), "png", pngFile); 
			System.out.println("### ETC1 texture is converted to .png. In order to repack it back for .sba, "
					+ "please provide .dds with the same name in " + FMT_R8G8B8 + " format.");
		} else {
			DDSImage image = DDSImage.createFromData(imageFormatId, width, height, mipMaps);
			fileName = fileName + FILE_DDS;
			image.write(new File(fileName));
		}
		System.out.println(String.format("### Texture unpacked: %s, format: %s, original format: %s, "
				+ "width: %d, height: %d, mipmaps: %d (original count: %d)", 
				fileName, formatName, curTexFormat.toString(), width, height, mipMaps.length, mipmapsOriginalCount));
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
			if (!checkSBAImageFormat(texParams)) { // All Mipmap objects repeats the image format
				System.out.println("!!! This Image format is not supported for repack (" + curTexFormat.toString() + "). Aborted.");
				return;
			}
			String nameAddition = textures.size() > 1 ? "_" + i : "";
			DDSImage image = loadImageData(nameAddition, width, height, mmmap.getMapElements().size());	
			
			int curMipmap = 0;
			for (String dataMMId : mmmap.getMapElements()) {
				int dataId = HEXUtils.strHexToInt(dataMMId);
				SBinDataElement mmLevel = SBJson.get().getDataElements().get(dataId);
				int mmLevelWidth = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, PARAM_WIDTH).getValue());
				int mmLevelHeight = Integer.parseInt(DataUtils.getDataFieldByName(mmLevel, PARAM_HEIGHT).getValue());
				String format = DataUtils.getDataFieldByName(mmLevel, PARAM_FORMAT).getValue();
				
				if (!checkImageFormat(format)) {
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
	// Image operations
	//
	
	private static DDSImage loadImageData(String nameAddition, int width, int height, int mmRequiredCount) throws IOException {
		DDSImage image = null;
		
		if (prevTexFormat.equals(SBinTextureFormat.ETC_RGB)) { // PNG from ETC1
			// Instead of converting ETC1 back, we're simply create a new RGB file
			// It should be compatible for all cases, despite of the big texture file size & device RAM consumption
			// Also we prevent the further Img compression loss done by converting textures back and forth
			// TODO However making a proper ETC1 .sba is possible
			System.out.println("### .sba format is " + prevTexFormat.toString() + ": save in RGB instead, looking for .dds file.");
		}
		image = DDSImage.read(new File(SBJson.get().getFileName() + nameAddition + FILE_DDS));
		setCurrentImageFormat(image.getPixelFormat());
		if (image.getWidth() != width || image.getHeight() != height) {
			System.out.println("!!! Image width, height or format is not compatible with the SBin data - expect broken Output file.");
		}
		return image;
	}
	
	// https://anilfilenet.wordpress.com/2011/01/22/flipping-an-image-horizontally-and-vertically-in-java/
	private static BufferedImage flipPNGPixelsVertically(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage dimg = new BufferedImage(w, h, img.getColorModel().getTransparency());
		Graphics2D g = dimg.createGraphics();
		g.drawImage(img, 0, 0, w, h, 0, h, w, 0, null);
		g.dispose();
		return dimg;
	}
	
	private static byte[] processMipmapImageOperations(
			byte[] mmData, int mmLevelWidth, int mmLevelHeight, boolean unpack) throws IOException {
		if (curTexFormat.equals(SBinTextureFormat.ETC_RGB)) {
			return mmData; // Do nothing
		}
		else if (unpack) {
			return flipDDSPixelsVertically(interleaveHexImage(mmData), mmLevelWidth, mmLevelHeight);
		} else {
			return interleaveHexImage(flipDDSPixelsVertically(mmData, mmLevelWidth, mmLevelHeight));
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
	private static byte[] flipDDSPixelsVertically(byte[] imageHex, int width, int height) {
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
	
	//
	// Utils
	//
	
	private static List<String> processDATAMipmapsList(SBinDataElement texParams, SBinDataElement mmmap) {
		return LaunchParameters.isMipmapUnpackDisabled() 
				|| curTexFormat.equals(SBinTextureFormat.ETC_RGB)
				? mmmap.getMapElements().subList(0, 1) : mmmap.getMapElements();
	}
	
	private static boolean checkSBAImageFormat(SBinDataElement texParams) {
		SBinDataElement mmmap = DataUtils.getDataElementFromValueId(texParams, PARAM_MIPMAPS);
		int dataId = HEXUtils.strHexToInt(mmmap.getMapElements().get(0));
		SBinDataElement mmLevel = SBJson.get().getDataElements().get(dataId);
		String imageFormat = DataUtils.getDataFieldByName(mmLevel, PARAM_FORMAT).getValue();
		
		curTexFormat = SBinTextureFormat.valueOf(imageFormat);
		boolean isSupported = false;
		switch(SBinTextureFormat.valueOf(imageFormat)) {
		case RGB: case ETC_RGB: // ETC_RGB is placed here for repacking reasons
			setCurrentImageFormat(DDSImage.D3DFMT_R8G8B8);
			setPrevSBAImageFormat(SBinTextureFormat.RGB);
			formatName = FMT_R8G8B8;
			isSupported = true;
			break;
		case RGBA: 
			setCurrentImageFormat(DDSImage.D3DFMT_A8R8G8B8);
			setPrevSBAImageFormat(SBinTextureFormat.RGBA);
			formatName = FMT_A8R8G8B8;
			isSupported = true;
			break;
		default: break;
		}
		return isSupported;
	}
	
	private static void setCurrentImageFormat(int imageFormat) {
		imageFormatId = imageFormat;
	}
	private static void setPrevSBAImageFormat(SBinTextureFormat format) {
		prevTexFormat = format;
	}
	
	private static boolean checkImageFormat(String dataFormatInfo) {
		boolean isCompatibleWithSBA = false;
		if ( (imageFormatId == DDSImage.D3DFMT_A8R8G8B8 
				&& SBinTextureFormat.valueOf(dataFormatInfo).equals(SBinTextureFormat.RGBA))
				|| (imageFormatId == DDSImage.D3DFMT_R8G8B8 
				&& SBinTextureFormat.valueOf(dataFormatInfo).equals(SBinTextureFormat.RGB)) ) {
			isCompatibleWithSBA = true;
		}
		return isCompatibleWithSBA;
	}
	
	public static void checkForImageFormatOperations() {
		// Replace ETC_RGB to RGB
		boolean changeImgFormat = false;
		List<SBinDataElement> textures = DataUtils.getAllDataElementsByStructName(PARAM_TEXTURE);
		
		for (SBinDataElement texParams : textures) {
			SBinDataElement mmmap = DataUtils.getDataElementFromValueId(texParams, PARAM_MIPMAPS);
			for (String dataMMId : mmmap.getMapElements()) {
				int dataId = HEXUtils.strHexToInt(dataMMId);
				SBinDataElement mmLevel = SBJson.get().getDataElements().get(dataId);
				SBinDataField formatField = DataUtils.getDataFieldByName(mmLevel, PARAM_FORMAT);
				if (formatField.getValue().contentEquals(SBinTextureFormat.ETC_RGB.toString())) {
					formatField.setValue(SBinTextureFormat.RGB.toString());
					if (!changeImgFormat) {changeImgFormat = true;}
				}
			}
		}
		if (changeImgFormat) {
			setPrevSBAImageFormat(SBinTextureFormat.ETC_RGB);
		}
	}
	
}
