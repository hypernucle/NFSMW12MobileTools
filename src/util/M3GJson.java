package util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import util.HEXClasses.M3GModel;
import util.HEXClasses.M3GObject;
import util.json.PolymorphDeserializer;

public class M3GJson {
	
	private static final Logger jl = Logger.getLogger(LogEntity.class.getSimpleName());
	
	static Gson gson = new Gson();
	
	private M3GJson() {}
	
	public static M3GModel loadM3GJson(String filePath) throws IOException {
		Reader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8);
		gson = new GsonBuilder()
				.registerTypeAdapter(M3GObject.class, new PolymorphDeserializer<M3GObject>())
				.create();
		M3GModel jsonObj = gson.fromJson(reader, new TypeToken<M3GModel>(){}.getType());
		reader.close();
		return jsonObj;
	}
	
	public static void outputSBJson(M3GModel m3g, String filePath) throws IOException {
		gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonString = gson.toJson(m3g);
		Files.write(Paths.get(filePath + ".json"), jsonString.getBytes(StandardCharsets.UTF_8));
	}
}
