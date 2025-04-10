import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import util.SBJson;
import util.SBinType;
import util.LaunchParameters;

public class FileCheck {
	
	public static void checkFiles(String folder, SBin sbin) throws IOException, InterruptedException {
		List<String> checkList = null;
		
		try (Stream<Path> stream = Files.list(Paths.get(folder))) {
	        checkList = stream.filter(file -> !Files.isDirectory(file))
	        		.map(Path::getFileName)
	        		.map(Path::toString)
	        		.collect(Collectors.toList());
	    }
		for (String origFile : checkList) {
			String origTotalPath = folder + origFile;
			System.out.println("### FileCheck for " + origFile + " : unpack/repack process started.");
			
			SBJson.initNewSBJson();
			SBin.setCurPos(0x0);
			SBJson.get().setSBinType(LaunchParameters.getSBinTypeByFileName(origFile));
			sbin.unpackSBin(SBinType.COMMON.toString(), origTotalPath, false);
			Path newFile = sbin.repackSBin(origTotalPath);
			
			boolean check = compareByMemoryMappedFiles(Paths.get(origTotalPath), newFile);
			System.out.println("### FileCheck for " + origFile + " : " + check + ".\n");
		}
	}
	
	// https://www.baeldung.com/java-compare-files
	public static boolean compareByMemoryMappedFiles(Path path1, Path path2) throws IOException {
	    try (RandomAccessFile randomAccessFile1 = new RandomAccessFile(path1.toFile(), "r"); 
	         RandomAccessFile randomAccessFile2 = new RandomAccessFile(path2.toFile(), "r")) {
	        
	        FileChannel ch1 = randomAccessFile1.getChannel();
	        FileChannel ch2 = randomAccessFile2.getChannel();
	        if (ch1.size() != ch2.size()) {
	            return false;
	        }
	        long size = ch1.size();
	        MappedByteBuffer m1 = ch1.map(FileChannel.MapMode.READ_ONLY, 0L, size);
	        MappedByteBuffer m2 = ch2.map(FileChannel.MapMode.READ_ONLY, 0L, size);

	        return m1.equals(m2);
	    }
	}
}
