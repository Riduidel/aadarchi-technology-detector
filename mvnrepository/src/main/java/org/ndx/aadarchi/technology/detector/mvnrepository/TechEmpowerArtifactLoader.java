package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import com.microsoft.playwright.Page;

public class TechEmpowerArtifactLoader extends ArtifactLoader {

	private Path techEmpowerFrameworks;
	MavenXpp3Reader reader = new MavenXpp3Reader();
	
	public TechEmpowerArtifactLoader(Path techEmpowerFrameworks) {
		this.techEmpowerFrameworks = techEmpowerFrameworks;
	}

	@Override
	public Set<ArtifactDetails> loadArtifacts(Page page)
			throws IOException {
		// TODO Auto-generated method stub
		File[] children = techEmpowerFrameworks.toFile().listFiles();
		if(children.length==0) {
			throw new UnsupportedOperationException(
					String.format("There are no children in %s", techEmpowerFrameworks.toString()));
		} else {
			return Stream.of(children)
				.filter(folder -> folder.getName().equals("Java"))
				.flatMap(javaFolder -> Stream.of(javaFolder.listFiles()))
				.filter(File::isDirectory)
				.flatMap(framework -> Arrays.asList(
						new File(framework, "pom.xml"),
						new File(framework, "build.gradle")
						).stream())
				.filter(File::exists)
				.flatMap(file -> 
					file.getName().equals("pom.xml") ? 
							identifyInterestingDependenciesInMaven(file).stream() :
								identifyInterestingDependenciesInGradle(file).stream())
				.collect(Collectors.toCollection(() -> new TreeSet<ArtifactDetails>()));
		}
	}
	
	Set<ArtifactDetails> identifyInterestingDependenciesInMaven(File pomFile) {
		Set<ArtifactDetails> returned = new TreeSet<ArtifactDetails>();
		try(InputStream input = new FileInputStream(pomFile)) {
			MavenProject mavenProject = new MavenProject(reader.read(input));
			mavenProject.getModel().getDependencies().stream()
				.filter(d -> !d.getGroupId().contains("${") && !d.getArtifactId().contains("${"))
				.map(d -> ArtifactDetailsBuilder.artifactDetails()
							.coordinates(String.format("%s:%s", d.getGroupId(), d.getArtifactId()))
							.build())
				.peek(a -> ExtractPopularMvnRepositoryArtifacts.logger.info("read artifact "+a))
				.forEach(a -> returned.add(a));
			// If pom has submodules, also explore them
			// (that could get us rid of the interesting_artifacts thingie
			mavenProject.getModel().getModules().stream()
				.forEach(module -> {
					var modulePom = new File(new File(pomFile.getParentFile(), module), "pom.xml");
					if(modulePom.exists()) {
						returned.addAll(identifyInterestingDependenciesInMaven(modulePom));
					}
				});
		} catch (IOException | XmlPullParserException e) {
			ExtractPopularMvnRepositoryArtifacts.logger.log(Level.SEVERE, e, () -> String.format("unable to get informations from pom %s", pomFile));
		}
		return returned;
	}
	
	Set<ArtifactDetails> identifyInterestingDependenciesInGradle(File folder) {
		Set<ArtifactDetails> returned = new TreeSet<>();
		ExtractPopularMvnRepositoryArtifacts.logger.severe("TODO implement handling of Gradle projects");
		return returned;
	}
}