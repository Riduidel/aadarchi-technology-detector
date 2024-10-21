package org.ndx.aadarchi.technology.detector.helper;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FileHelper {
	public static final Logger logger = Logger.getLogger(FileHelper.class.getName());
	
	private static ObjectMapper objectMapper;
	
	static {
		objectMapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		objectMapper.findAndRegisterModules();
	}

	
	public static <Type> Type readFromFile(File file, TypeReference<Type> type) throws IOException {
		return objectMapper.readValue(FileUtils.readFileToString(file, "UTF-8"),
				type);
	}

	public static void writeToFile(Object allDetails, File file) throws IOException {
		logger.fine("Exporting artifacts to " + file.getAbsolutePath());
		FileUtils.write(file, 
				objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allDetails), 
				"UTF-8");
	}

	public static ObjectMapper getObjectMapper() {
		return objectMapper;
	}

}
