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
import org.ndx.aadarchi.technology.detector.helper.Utils;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
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
	public final File mavenExecutable;
	public final File mavenPropertiesCache;

	public MvnContext(BrowserContext context, File maven, Path cache) {
		super();
		this.context = context;
		this.mavenExecutable  = maven;
		this.mavenPropertiesCache = new File(cache.toAbsolutePath().toFile(), "maven/properties");
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
}