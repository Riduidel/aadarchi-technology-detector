package org.ndx.aadarchi.technology.detector.helper;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class FileHelper {
	public static final Logger logger = Logger.getLogger(FileHelper.class.getName());

	public static List<ArtifactDetails> readFromFile(File file) throws IOException {
		return gson.fromJson(FileUtils.readFileToString(file, "UTF-8"),
				new TypeToken<List<ArtifactDetails>>() {});
	}

	public static void writeToFile(Collection<ArtifactDetails> allDetails, File file) throws IOException {
		logger.info("Exporting artifacts to " + file.getAbsolutePath());
		FileUtils.write(file, gson.toJson(allDetails), "UTF-8");
		logger.info(String.format("Exported %d artifacts to %s", allDetails.size(), file));
	}

	public static Gson gson = new GsonBuilder()
	.setPrettyPrinting()
	.create();

}
