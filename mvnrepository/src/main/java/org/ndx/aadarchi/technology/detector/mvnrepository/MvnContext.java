package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.ndx.aadarchi.technology.detector.helper.ExtractionContext;
import org.ndx.aadarchi.technology.detector.helper.Utils;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetailsBuilder;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;

public class MvnContext implements ExtractionContext {
	public static final Logger logger = Logger.getLogger(MvnContext.class.getName());
	public final BrowserContext context;
	private String artifactDetailsExtractor;
	private Invoker invoker = new DefaultInvoker();
	private File mavenExecutable;
	private File mavenPropertiesCache;

	public MvnContext(BrowserContext context, File maven, Path cache) {
		super();
		this.context = context;
		this.mavenExecutable  = maven;
		this.mavenPropertiesCache = new File(cache.toAbsolutePath().toFile(), "maven/properties");
	}

	private String executeMavenCommand(Optional<File> directory, List<String> arguments) {
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
					request.setMavenExecutable(mavenExecutable);
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
						logger.info(message.toString());
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

	public Page newPage() {
		return newPage(context);
	}

	private String getArtifactDetailsExtractor() {
		if (artifactDetailsExtractor == null) {
			try {
				artifactDetailsExtractor = IOUtils.toString(MvnArtifactLoaderHelper.class.getClassLoader()
						.getResourceAsStream("artifact_details_extractor.js"), "UTF-8");
			} catch (IOException e) {
				throw new UnsupportedOperationException("can't read file");
			}
		}
		return artifactDetailsExtractor;
	}

	/**
	 * Creates a correctly configured page
	 * 
	 * @param context
	 * @return
	 */
	private static Page newPage(BrowserContext context) {
		Page created = context.newPage();
		created.onConsoleMessage(message -> {
			Level level = switch (message.type()) {
			case "debug" -> Level.FINE;
			case "warning" -> Level.WARNING;
			case "error" -> Level.SEVERE;
			default -> Level.INFO;
			};
			logger.logp(level, "Playwright", message.location(), () -> message.text());
		});
		return created;
	}

	/**
	 * Obtains basic details for artifact url
	 * 
	 * @param url
	 * @return
	 */
	public Optional<Map> addDetails(String url) {
		Page page = newPage();
		try {
			Response response = page.navigate(url, new NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
			Map pageInfos = null;
			if (response != null && response.status() < 300) {
				logger.info(String.format("Reading details of %s", url));
				pageInfos = ((Map) page.evaluate(getArtifactDetailsExtractor()));
			} else {
				logger.warning(String.format("Failed to load %s, status is %s", url, response.status()));
			}
			return Optional.ofNullable(pageInfos);
		} finally {
			page.close();
		}
	}

	public static String getArtifactUrl(ArtifactDetails details, String mvnRepositoryServer) {
		return String.format("%s/artifact/%s/%s", mvnRepositoryServer, details.getGroupId(), details.getArtifactId());
	}

	/**
	 * Add some complementary details by, unfortunatly, loading the maven pom and
	 * analyzing it thanks to maven itself.
	 * 
	 * @param source
	 * @param mvnRepositoryServer
	 */
	public ArtifactDetails addMavenDetails(ArtifactDetails source, String mvnRepositoryServer) {
		ArtifactDetailsBuilder builder = ArtifactDetailsBuilder.toBuilder(source);
		builder = addMavenModelDetails(source, builder);
		return builder.build();
	}

	private ArtifactDetailsBuilder addMavenModelDetails(ArtifactDetails source, ArtifactDetailsBuilder builder) {
		if(source.getRepositories().contains("Central")) {
			try {
				// TODO add preloaded properties
				Map<String, String> preloaded = getPreloadedProperties(source);
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
			logger.warning(
					String.format(
							"Artifact %s is not present in maven central but in %s."
							+ "Please update maven settings to add the required repositories",
							source.getCoordinates(),
							source.getRepositories()));
		}
		return builder;
	}

	private Map<String, String> getPreloadedProperties(ArtifactDetails updated) {
		File propertiesCache = new File(mavenPropertiesCache, 
				updated.getCoordinates()
				.replace(":", "/")
				.replace(".", "/")
				+"."
				+updated.getVersions().lastKey()
				+".properties");
		if(!propertiesCache.exists()) {
			propertiesCache.getParentFile().mkdirs();
			// Now build the properties
			Map<String, String> properties = getInterestingProperties(updated);
			Properties p = new Properties();
			p.putAll(properties);
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
				Map<String, String> returned = new TreeMap<String, String>(); 
				for(Map.Entry<?, ?> e  : used.entrySet()) {
					String k = e.getKey().toString();
					String v = e.getValue().toString();
					if("null object or invalid expression".equals(v)) {
						v = null;
					}
					returned.put(k, v);
				}
				return returned;
			}
		} catch(IOException e) {
			throw new RuntimeException("Unable to read properties cache at "+propertiesCache.getAbsolutePath(), e);
		}
	}

	private Map<String, String> getInterestingProperties(ArtifactDetails artifact) {
		Map<String, String> returned = new TreeMap<String, String>();
		for(String text : Arrays.asList("project.url", "project.description", "project.scm.url")) {
			String value = executeMavenCommand(Optional.empty(), 
					Arrays.asList(
							"help:evaluate",
							"-Dartifact="+artifact.getCoordinates()+":"+artifact.getVersions().lastKey(),
							"-Dexpression="+text,
							"--quiet",
							"-DforceStdout"));
			returned.put(text, value.trim());
		}
		return returned;
	}
}