package org.ndx.aadarchi.technology.detector.helper;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class FileHelper {
	public static final Logger logger = Logger.getLogger(FileHelper.class.getName());
	
	private static ObjectMapper objectMapper;
	
	static {
		objectMapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				;
	}

	
	public static List<ArtifactDetails> readFromFile(File file) throws IOException {
		return objectMapper.readValue(FileUtils.readFileToString(file, "UTF-8"),
				new TypeReference<List<ArtifactDetails>>() {});
	}

	public static void writeToFile(Collection<ArtifactDetails> allDetails, File file) throws IOException {
		logger.fine("Exporting artifacts to " + file.getAbsolutePath());
		FileUtils.write(file, 
				objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allDetails), 
				"UTF-8");
		logger.info(String.format("Exported %d artifacts to %s", allDetails.size(), file));
	}

	public static ObjectMapper getObjectMapper() {
		return objectMapper;
	}

}
