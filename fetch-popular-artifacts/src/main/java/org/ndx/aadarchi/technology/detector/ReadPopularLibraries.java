package org.ndx.aadarchi.technology.detector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ndx.aadarchi.technology.detector.librariesio.LibrariesIOClient;
import org.ndx.aadarchi.technology.detector.librariesio.model.Platform;
import org.ndx.aadarchi.technology.detector.librariesio.model.Project;
import org.ndx.aadarchi.technology.detector.model.Technology;
import org.ndx.aadarchi.technology.detector.model.TechnologyRepository;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReadPopularLibraries extends RouteBuilder {
	@ConfigProperty(name = "projects_per_page", defaultValue = "10")
	private int projectsPerPage;
	@ConfigProperty(name="projects_per_platform", defaultValue = "1000")
	private int projectsPerPlatform;
	// TODO convert to file
	@ConfigProperty(name="storage-folder", defaultValue="./target/storage")
	private String storageFolderPath;
	@ConfigProperty(name="libraries-list", defaultValue="libraries.json")
	private String librariesListPath;
	@ConfigProperty(name="rejected-platforms", defaultValue="Bower,Carthage,Alcatraz,SwiftPM,Nimble,PureScript")
	private List<String> rejectedPlatforms;
	
	@Inject TechnologyRepository technologies;
	
	private class ReaggregateListsOfLibraries implements AggregationStrategy {

		@Override
		public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
			List returned = oldExchange==null ? new ArrayList() : (List) oldExchange.getMessage().getBody();
			Object body = newExchange.getMessage().getBody();
			if (body instanceof Collection) {
				Collection c = (Collection) body;
				returned.addAll(c);
			} else {
				returned.add(body);
			}
			return ExchangeBuilder.anExchange(getCamelContext())
					.withBody(returned)
					.build();
		}
		
	}
	
	@PostConstruct
	public void initializeH2WithCSV() {
		Log.error("TODO Implement csv loading at startup");
	}
	
	@PreDestroy
	public void dumpH2ToCSV() {
		Log.error("TODO Implement csv dumping at shutdown");
	}
	
	@RestClient LibrariesIOClient librariesIo;
	
    @Override
    public void configure() throws Exception {
    	getContext().setManagementName(getClass().getSimpleName());
    	from("timer:autostart?repeatCount=1")
    		.routeId("1-get-all-platforms")
    		.description("Get all libraries platforms from libraries.io")
    		.process(this::getPlatforms)
    		.split(body(), new ReaggregateListsOfLibraries())
	    		.choice()
	    		.when(this::isSupportedPlatform)
	    			.log(LoggingLevel.INFO, "Processing libraries of ${body.name}")
		    		.description("Split per deployment platform")
		    		.to("direct:get-all-libraries-of-platform")
		    	.otherwise()
		    		.log(LoggingLevel.WARN, "Platform ${body.name} is rejected")
		    		.stop()
    		.end()
	    	.log(LoggingLevel.INFO, "All libraries of platforms have been injected. Now computing indicators (from a different route)")
	    	;
    	
    	from("direct:get-all-libraries-of-platform")
    		.routeId("2-get-all-libraries-of-platform")
    		.description("Get the first n libraries for the given platform from libraries.io.\n"
    				+ "Notice this also downloads all details (versions and so on) from libraries to put them in cache ... somewhere")
    		.process(this::readTheLibrariesForTheGivenPlatform)
    		.split(body(), AggregationStrategies.groupedBody())
	    		.description("Split per library")
	    		.to("direct:convert-library-infos")
	    		.end()
	    	;
    	
    	from("direct:convert-library-infos")
    		.routeId("3-convert-library-to-technology-entity")
    		.description("Convert library infos to technology object and save it immediatly")
    		.process(this::convertLibrariesIOLibraryToTechnology)
    		// Library processing is over, we no more need to do anything
    		;
    }
    
    public void readTheLibrariesForTheGivenPlatform(Exchange exchange) {
    	List<Project> allLibraries = new ArrayList<Project>();
    	String platform = (exchange.getMessage().getBody(Platform.class)).getName();
    	boolean hasNextPage = true;
		// Now read the 1000 first libraries for that platform
    	for (int i = 1; allLibraries.size()<projectsPerPlatform && hasNextPage; i++) {
			List<Project> libraries = librariesIo.searchProjects(i, projectsPerPage, platform);
			allLibraries.addAll(libraries);
			hasNextPage = libraries.size()==projectsPerPage;
		}
    	exchange.getMessage().setBody(allLibraries);
    	Log.infof("Loaded %d libraries for platform %s", allLibraries.size(), platform);
    }

	private void getPlatforms(Exchange exchange) {
		exchange.getMessage().setBody(librariesIo.getPlatforms());
	}

	private void convertLibrariesIOLibraryToTechnology(Exchange exchange) {
		Technology returned = technologies.findOrCreateFromLibrariesIOLibrary((Project) exchange.getMessage().getBody());
		exchange.getMessage().setBody(returned);
	}

	private boolean isSupportedPlatform(Exchange exchange) {
		Platform platform = exchange.getMessage().getBody(Platform.class);
		return isSupportedPlatform(platform);
	}

	/**
	 * Filter some unsupported platforms (as defined in configuration)
	 * @param platform
	 * @return true if platform is not in 
	 */
	public boolean isSupportedPlatform(Platform platform) {
		return !rejectedPlatforms.contains(platform.getName());
	}
}