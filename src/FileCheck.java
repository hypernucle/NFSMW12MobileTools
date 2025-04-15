import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Checksum;

import util.SBJson;
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
		int failedCount = 0;
		for (String origFile : checkList) {
			String origTotalPath = folder + origFile;
			
			SBJson.initNewSBJson();
			SBin.setCurPos(0x0);
			SBJson.get().setSBinType(LaunchParameters.getSBinTypeByFileName(origFile));
			Checksum origCRC = sbin.unpackSBin(origTotalPath, false);
			Checksum newCRC = sbin.repackSBin(origTotalPath, false);
			
			boolean check = origCRC.getValue() == newCRC.getValue();
			System.out.println("### FileCheck for " + origFile + " : " + check + ".");
			if (!check) {failedCount++;}
		}
		if (failedCount != 0) {
			System.out.println("\n### FileCheck failed with " + failedCount + " files during re-compilation.");
		}
	}
	

}
