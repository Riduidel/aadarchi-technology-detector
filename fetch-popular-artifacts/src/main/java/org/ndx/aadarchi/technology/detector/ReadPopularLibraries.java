package org.ndx.aadarchi.technology.detector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ndx.aadarchi.technology.detector.librariesio.LibrariesIOClient;
import org.ndx.aadarchi.technology.detector.librariesio.model.Platform;
import org.ndx.aadarchi.technology.detector.librariesio.model.Project;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

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
	
	private class ReaggregateListsOfLibraries implements AggregationStrategy {

		@Override
		public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
			List returned = oldExchange==null ? new ArrayList() : (List) oldExchange.getMessage().getBody();
			returned.addAll((Collection) newExchange.getMessage().getBody());
			return ExchangeBuilder.anExchange(getCamelContext())
					.withBody(returned)
					.build();
		}
		
	}
	
	private class AggregateLibraryInfosAsMap implements AggregationStrategy {

		public static final String LIBRARY_INFOS_KEY = "LIBRARY_INFOS_KEY";

		@Override
		public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
			Map<String, Object> returned = oldExchange==null ? new TreeMap<String, Object>() : (Map<String, Object>) oldExchange.getMessage().getBody();
			String key = newExchange.getMessage().getHeader(LIBRARY_INFOS_KEY, String.class);
			returned.put(key, newExchange.getMessage().getBody());
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
	    		.description("Split per deployment platform")
	    		.to("direct:get-all-libraries-of-platform")
	    		.end()
	    	// Now we have a list of maps, let's sort them!
    		.setHeader("CamelFileName", constant("libraries.json"))
    		.log("Writing libraries to ${headers.CamelFileName}")
    		// I do prefer to have whitespaces
    		.marshal().json(true)
    		.to(String.format("file://%s", storageFolderPath))
	    	;
    	
    	from("direct:get-all-libraries-of-platform")
    		.routeId("2-get-all-libraries-of-platform")
    		.description("Get the first n libraries for the given platform from libraries.io.\n"
    				+ "Notice this also downloads all details (versions and so on) from libraries to put them in cache ... somewhere")
    		.process(this::readTheLibrariesForTheGivenPlatform)
    		.split(body(), AggregationStrategies.groupedBody())
	    		.description("Split per library")
	    		.to("direct:extract-library-infos")
	    		.end()
	    	;
    	
    	from("direct:extract-library-infos")
    		.routeId("3-extract-library-infos")
    		.description("Saves a JSON file containing library infos then export the library GUID")
    		.multicast(new AggregateLibraryInfosAsMap())
    			.to("direct:get-package-manager-url")
    			.to("direct:save-library-infos")
    		.end()
    		;
    	
    	from("direct:get-package-manager-url")
    		.routeId("4-get-package-manager-url")
    		.setBody(simple("${body.packageManagerUrl}"))
    		.choice()
    		// This is not good since we filter out dependencies of some
    		// package managers (swift and so on)
    		  .when(body().isEqualTo(null))
    		   .stop()
    		.end()
    		.setHeader(AggregateLibraryInfosAsMap.LIBRARY_INFOS_KEY, constant("packageManagerUrl"))
    		;
    	from("direct:save-library-infos")
    		.routeId("5-save-library-infos")
    		.log("Writing ${body}")
    		// TODO find a way to use csimple (which won't run interpreter at runtime but rather compile some code)
    		.setHeader("CamelFileName", simple("libraries/${body.platform}/${body.name}.json"))
    		// I do prefer to have whitespaces
    		.marshal().json(true)
    		.to(String.format("file://%s", storageFolderPath))
    		.setHeader(AggregateLibraryInfosAsMap.LIBRARY_INFOS_KEY, constant("cachePath"))
    		.setBody(simple("${headers.CamelFileName}"))
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
    }

	private void getPlatforms(Exchange exchange) {
		exchange.getMessage().setBody(librariesIo.getPlatforms());
	}
}