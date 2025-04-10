package util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import util.DataClasses.SBinField;
import util.DataClasses.SBinHCStructFileArray;
import util.DataClasses.SBinJson;
import util.DataClasses.SBinStruct;
import util.SBinHCStructs.SBinHCStruct;
import util.json.PolymorphDeserializer;

public class SBJson {
	
	static Gson gson = new Gson();
	private static SBinJson sbinJsonEnt;
	private static SBinHCStructFileArray hcStructFileArray;
	
	private SBJson() {}
	
	public static SBinJson get() {
		return sbinJsonEnt;
	}
	public static List<String> getHCStructFileArray() {
		return hcStructFileArray.getFileNames();
	}
	
	public static void initNewSBJson() throws IOException {
		sbinJsonEnt = new SBinJson();
		loadHCStructsFileArray();
	}
	
	public static void loadSBJson(String filePath) throws IOException {
		Reader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
		gson = new GsonBuilder()
				.registerTypeAdapter(SBinHCStruct.class, new PolymorphDeserializer<SBinHCStruct>())
				.create();
		SBinJson sbinJsonObj = gson.fromJson(reader, new TypeToken<SBinJson>(){}.getType());
		reader.close();
		sbinJsonEnt = sbinJsonObj;
	}
	
	private static void loadHCStructsFileArray() throws IOException {
		Reader reader = Files.newBufferedReader(Paths.get("HCStructFileArray.json"), StandardCharsets.UTF_8);
		SBinHCStructFileArray hcStructFiles = gson.fromJson(reader, new TypeToken<SBinHCStructFileArray>(){}.getType());
		//System.out.println(hcStructFiles.getFileNames().size());
		reader.close();
		hcStructFileArray = hcStructFiles;
	}
	
	public static void outputSBJson() throws IOException {
		gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonOut = gson.toJson(SBJson.get());
		Files.write(Paths.get(SBJson.get().getFileName() + ".json"), jsonOut.getBytes(StandardCharsets.UTF_8));
	}
	
	public static void clearJsonOutputStuff() {
		for (SBinStruct struct : SBJson.get().getStructs()) {
			if (!struct.getFieldsArray().isEmpty()) {
				for (SBinField field : struct.getFieldsArray()) {
					field.setFieldTypeEnum(null);
				}
			}
		}
		for (SBinField emptyField : SBJson.get().getEmptyFields()) {
			emptyField.setFieldTypeEnum(null);
		}
		// If we know the objects from all SBin blocks, we can properly re-build
		// the entire CDAT strings order 1-to-1 like original file. 
		// The first empty entry must be kept though, since it could be found on random blocks of the file
		if (SBJson.get().isCDATAllStringsFromDATA()) {
			SBJson.get().setCDATStrings(SBJson.get().getCDATStrings().subList(0, 1));
		}
	}
}
