package org.ndx.aadarchi.technology.detector.helper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FileHelperTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = FileHelper.getObjectMapper();
    }

    @Test
    public void testWriteToFile_Success(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.json").toFile();
        Object allDetails = new TestObject("someValue", null);

        FileHelper.writeToFile(allDetails, file);

        String fileContent = Files.readString(file.toPath());
        assertTrue(fileContent.contains("someValue"));
        assertFalse(fileContent.contains("nullField"));
    }

    @Test
    public void testReadFromFile_Success(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.json").toFile();
        String fileContent = "{\"nonNullField\":\"someValue\"}";
        FileUtils.writeStringToFile(file, fileContent, "UTF-8");

        TypeReference<TestObject> typeReference = new TypeReference<>() {};
        TestObject result = FileHelper.readFromFile(file, typeReference);

        assertNotNull(result);
        assertEquals("someValue", result.getNonNullField());
    }

    @Test
    public void testObjectMapperDoesNotWriteNullValues(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.json").toFile();
        TestObject testObject = new TestObject("someValue", null);

        FileHelper.writeToFile(testObject, file);

        String fileContent = Files.readString(file.toPath());
        assertTrue(fileContent.contains("someValue"));
        assertFalse(fileContent.contains("nullField"));
    }

    private static class TestObject {
        private String nonNullField;
        private String nullField;

        @JsonCreator
        public TestObject(@JsonProperty("nonNullField") String nonNullField,
                          @JsonProperty("nullField") String nullField) {
            this.nonNullField = nonNullField;
            this.nullField = nullField;
        }

        public String getNonNullField() {
            return nonNullField;
        }

        public void setNonNullField(String nonNullField) {
            this.nonNullField = nonNullField;
        }

        public String getNullField() {
            return nullField;
        }

        public void setNullField(String nullField) {
            this.nullField = nullField;
        }
    }
}
