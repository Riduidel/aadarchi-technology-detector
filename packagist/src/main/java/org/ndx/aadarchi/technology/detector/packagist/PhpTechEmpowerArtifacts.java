package org.ndx.aadarchi.technology.detector.packagist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.loader.TechEmpowerArtifactLoader;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PhpTechEmpowerArtifacts extends TechEmpowerArtifactLoader<PackagistContext> {
    private static final String PHP_LANGUAGE_FOLDER = "php";

    public PhpTechEmpowerArtifacts(Path cache, Path techEmpowerFrameworks) {
        super(cache, techEmpowerFrameworks, PHP_LANGUAGE_FOLDER);
    }

    @Override
    protected Collection<ArtifactDetails> locateArtifactsIn(Stream<File> matchingFrameworksFolders) {
        return matchingFrameworksFolders
                .flatMap(framework -> Stream.of(
                        new File(framework, "composer.json")
                ))
                .filter(File::exists)
                .map(this::toArtifactDetails)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    protected Collection<ArtifactDetails> toArtifactDetails(File composerJson) {
        try {
            ObjectMapper objectMapper = FileHelper.getObjectMapper();
            String content = FileUtils.readFileToString(composerJson, "UTF-8");
            Map<String, Object> composerData = objectMapper.readValue(content, Map.class);
            Collection<ArtifactDetails> returned = new TreeSet<>();
            if (composerData.containsKey("require")) {
                @SuppressWarnings("unchecked")
                Map<String, String> dependencies = (Map<String, String>) composerData.get("require");
                dependencies.keySet().stream()
                        .map(dep -> ArtifactDetailsBuilder.artifactDetails().name(dep).build())
                        .forEach(returned::add);
            }
            return returned;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Can't read composer.json file %s", composerJson), e);
        }
    }
}
