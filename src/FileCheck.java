import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Checksum;

import util.SBJson;
import util.LaunchParameters;
import util.LogEntity;

public class FileCheck {
	
	private static final Logger jl = Logger.getLogger(LogEntity.class.getSimpleName());
	
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
			jl.log(Level.FINE, "FileCheck for {0}: {1}.", new Object[] {origFile, check});
			if (!check) {failedCount++;}
		}
		if (failedCount != 0) {
			jl.log(Level.WARNING, "FileCheck failed with {0} files during re-compilation.", failedCount);
		}
	}
	

}
