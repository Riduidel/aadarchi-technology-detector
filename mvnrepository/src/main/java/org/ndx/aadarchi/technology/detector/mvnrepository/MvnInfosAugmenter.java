package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.ndx.aadarchi.technology.detector.augmenters.Augmenter;
import org.ndx.aadarchi.technology.detector.helper.Utils;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import io.github.emilyydev.asp.ProvidesService;

/**
 * Read infos from maven to have them inserted into artifact details
 */
@ProvidesService(Augmenter.class)
public class MvnInfosAugmenter implements Augmenter {
	private static final String NULL_MAVEN_VALUE = "null object or invalid expression";
	public static final Logger logger = Logger.getLogger(MvnInfosAugmenter.class.getName());
	private Invoker invoker = new DefaultInvoker();
	@Override
		public int order() {
			return 1;
		}

	@Override
	public ArtifactDetails augment(ExtractionContext context, ArtifactDetails source, Date date) {
		ArtifactDetailsBuilder builder = ArtifactDetailsBuilder.toBuilder(source);
		if(source.getCoordinates()==null) {
			logger.severe(String.format("Unable to add maven infos for %s\n(coordinates are null)", source));
		} else {
			if (context instanceof MvnContext) {
				MvnContext mvnContext = (MvnContext) context;
				builder = addMavenModelDetails(mvnContext, source, builder);
			}
		}
		return builder.build();
	}


	private ArtifactDetailsBuilder addMavenModelDetails(MvnContext context, ArtifactDetails source, ArtifactDetailsBuilder builder) {
		if(source.getRepositories()==null || source.getRepositories().isEmpty() || source.getRepositories().contains("Central")) {
			try {
				Map<String, String> preloaded = getPreloadedProperties(context, source);
				String description = preloaded.get("project.description");
				if(description!=null && description.length()>source.getDescription().length()) {
					builder = builder.description(description);
				}
				Map<String, String> urls = new TreeMap<String, String>();
				String url = preloaded.get("project.scm.url");
				if(url!=null) {
					try {
						url = url.replace("git@github.com:", "https://github.com/");
						url = url.replace("github.com:", "github.com/");
						url = url.replaceAll("(scm:)?(git:)?(.*).git.*", "$3");
						urls.put(Utils.getDomain(url), url);
					} catch(RuntimeException e) {
						// Just ignore the bad encoding hidden below runtime exception
						logger.warning("Unable to parse scm url "+url);
					}
				}
				url = preloaded.get("project.url");
				if(url!=null) {
					urls.put(Utils.getDomain(url), url);
				}
				if(urls.size()>0) {
					builder = builder.urls(urls);
				}
			} catch(RuntimeException e) {
				logger.log(Level.WARNING, "Unable to prefil properties for "+source.getCoordinates(), e);
			}
		} else {
			logger.fine(
					String.format(
							"Artifact %s is not present in maven central but in %s."
							+ "Please update maven settings to add the required repositories",
							source.getCoordinates(),
							source.getRepositories()));
		}
		return builder;
	}
	
	private Map<String, String> getPreloadedProperties(MvnContext context, ArtifactDetails updated) {
		File propertiesCache = new File(context.mavenPropertiesCache, 
				updated.getCoordinates()
				.replace(":", "/")
				.replace(".", "/")
				+(updated.getVersions()==null || updated.getVersions().isEmpty() ? "" : "."+updated.getVersions().lastKey()) 
				+".properties");
		if(!propertiesCache.exists()) {
			propertiesCache.getParentFile().mkdirs();
			// Now build the properties
			Properties p = new Properties();
			try {
				Map<String, String> properties = getInterestingProperties(context, updated);
				p.putAll(properties);
			} catch(RuntimeException e) {
				logger.warning("Unable to populate property cache for "+updated);
			}
			try {
				try(FileOutputStream output = new FileOutputStream(propertiesCache)) {
					p.store(output, null);
				}
			} catch(IOException e) {
				throw new RuntimeException("Unable to write properties cache at "+propertiesCache.getAbsolutePath(), e);
			}
		}
		Properties used = new Properties();
		try {
			try(FileInputStream input = new FileInputStream(propertiesCache)) {
				used.load(input);
				return used.entrySet()
					.stream()
					.filter(e -> e.getValue()!=null && !NULL_MAVEN_VALUE.equals(e.getValue()))
					.map(e -> Map.entry(e.getKey().toString(), e.getValue().toString()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			}
		} catch(IOException e) {
			throw new RuntimeException("Unable to read properties cache at "+propertiesCache.getAbsolutePath(), e);
		}
	}


	private Map<String, String> getInterestingProperties(MvnContext context, ArtifactDetails artifact) {
		logger.info("Fetching interesting properties of "
				+artifact.getCoordinates()
				+ (artifact.getVersions()==null || artifact.getVersions().isEmpty() ? "" :  ":"+artifact.getVersions().lastKey()));
		Map<String, String> returned = new TreeMap<String, String>();
		for(String text : Arrays.asList("project.url", "project.description", "project.scm.url")) {
			String value = executeMavenCommand(context, Optional.empty(), 
					Arrays.asList(
							"help:evaluate",
							"-Dartifact="+artifact.getCoordinates()+ (artifact.getVersions().isEmpty() ? "" :  ":"+artifact.getVersions().lastKey()),
							"-Dexpression="+text,
							"--quiet",
							"-DforceStdout"));
			if(!NULL_MAVEN_VALUE.equals(value)) {
				returned.put(text, value.trim());
			}
		}
		return returned;
	}

	private String executeMavenCommand(MvnContext context, Optional<File> directory, List<String> arguments) {
		try {
			try(
				ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				try(
					PrintStream outputPrinter = new PrintStream(outputStream);
					PrintStream errorPrinter = new PrintStream(errorStream);
					) {
					InvocationRequest request = new DefaultInvocationRequest();
					request.setOutputHandler(new PrintStreamHandler(outputPrinter, false));
					request.setErrorHandler(new PrintStreamHandler(errorPrinter, false));
					request.setMavenExecutable(context.mavenExecutable);
					directory.ifPresent(dir -> request.setBaseDirectory(dir));
					request.addArgs(arguments);
					try {
						InvocationResult result = invoker.execute(request);
						String output = outputStream.toString("UTF-8").trim();
						String error = errorStream.toString("UTF-8").trim();
						String loggedCommand = " mvn "+arguments.stream().collect(Collectors.joining(" "));
						StringBuilder message = new StringBuilder()
								.append("Running \"").append(loggedCommand).append("\"\n")
								;
						if(output.length()>0)
							message = message
								.append("Has output\n===============\n")
								.append(output)
								.append("\n===============\n");
						if(error.length()>0)
							message = message
								.append("Has error\n===============\n")
								.append(error)
								.append("\n===============\n");
						logger.fine(message.toString());
						if(result.getExitCode()==0) {
							return output;
						} else {
							throw new RuntimeException(
									String.format("Unable to process command \"%s\"\nError is\n%s",loggedCommand, error));
						}
					} catch (MavenInvocationException e) {
						throw new RuntimeException("Unable to run maven command "+request.toString(), e);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("An IOException for a ByteArrayOutputStream?", e);
		}
	}

}
