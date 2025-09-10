package org.ndx.aadarchi.technology.detector;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ndx.aadarchi.technology.detector.librariesio.LibrariesIOClient;
import org.ndx.aadarchi.technology.detector.librariesio.model.Platform;
import org.ndx.aadarchi.technology.detector.model.Technology;
import org.ndx.aadarchi.technology.detector.model.TechnologyRepository;
import org.ndx.aadarchi.technology.detector.processors.TechnologyRepositoryProcessor;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AddMissingFields extends EndpointRouteBuilder {
	private static final String ADD_MISSING_FIELDS = "direct:add-missing-technologies-fields";

	@ConfigProperty(name="rejected-platforms", defaultValue="Bower,Carthage,Alcatraz,SwiftPM,Nimble,PureScript")
	private List<String> rejectedPlatforms;
	@RestClient LibrariesIOClient librariesIo;

	TechnologyRepositoryProcessor technologies;

	@Inject
	public void setTechnologies(TechnologyRepositoryProcessor technologies) {
		this.technologies = technologies;
	}

	private Map<String, String> platformMappings;
	
    @Override
    public void configure() throws Exception {
    	from(generateStarterEndpoint())
    		.routeId(getClass().getSimpleName()+"-1-get-all-technologies")
    		.description("Get all technologies")
			.log("🔍 Searching for technologies")
			// Load all technologies
			// I think it will be necessary to have some kind of batch processing
			.process(technologies::findAllTechnologies)
			.log("✅  Found ${body.size} technologies")
			.split(body())
//				.parallelProcessing()
				.to(ADD_MISSING_FIELDS)
				.end()
			.log("🎉 All indicators computations have been created, now searching them by date")
	    	;
    	from(ADD_MISSING_FIELDS)
			.routeId(getClass().getSimpleName()+"-2-add-missing-fields")
			.description("Add missing fields to technology")
			.process(this::addPlatform)
			    		;
    }

	private EndpointConsumerBuilder generateStarterEndpoint() {
		return direct(getClass().getSimpleName());
	}

	private void addPlatform(Exchange exchange1) {
		Technology body = (Technology) exchange1.getMessage().getBody();
		if(body.platform==null) {
			Log.infof("🚚 Adding missing platform to %s", body);
			Technology technology = addPlatform(body);
			exchange1.getMessage().setBody(technology);
		}
	}

	/**
	 * Query libraries.io to find all projects having the given
	 * repository url.
	 * THIS QUERY SHOULD HAVE ITS RESULTS CACHED!
	 * @param body
	 * @return
	 */
	private Technology addPlatform(Technology body) {
		if(body.packageManagerUrl!=null) {
			return addPlatformByPackageManagerUrl(body);
			
		} else {
			Log.errorf("Unable to find a way to get platform of %s", body);
		}
		return body;
	}

	private Technology addPlatformByPackageManagerUrl(Technology body) {
		try {
			URI url = new URI(body.packageManagerUrl);
			String host = url.getHost();
			Optional<String> platformName = getPlatformMappings().entrySet().stream()
				.filter(entry -> host.contains(entry.getKey()))
				.map(Map.Entry::getValue)
				.findFirst();
			if(platformName.isPresent()) {
				body.platform = platformName.get();
				return technologies.update(body, t -> t.platform = platformName.get());
			} else {
				Log.warnf("We couldn't find the platform of %s", body);
				return body;
			}
		} catch (URISyntaxException e) {
			Log.errorf(e, "Unable to create an URI from repository url %s (from technology %s)", e, body.repositoryUrl, body);
			return body;
		}
	}

	private Map<String, String> getPlatformMappings() {
		if(platformMappings==null) {
			platformMappings = createPlatformMappings();
		}
		return platformMappings;		
	}

	private synchronized Map<String, String> createPlatformMappings() {
		List<Platform> platforms = librariesIo.getPlatforms();
		return platforms.stream()
				.filter(p -> !rejectedPlatforms.contains(p.getName()))
				.collect(Collectors.toMap(p -> getDomain(p), Platform::getName));
	}
	
	public String getDomain(Platform p) {
		try {
			URI uri = new URI(p.getHomepage());
			return uri.getHost();
		} catch (URISyntaxException e) {
			throw new RuntimeException("TODO handle URISyntaxException", e);
		}
	}
}