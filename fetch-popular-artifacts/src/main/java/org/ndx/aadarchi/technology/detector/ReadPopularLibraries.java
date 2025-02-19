package org.ndx.aadarchi.technology.detector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
			Exchange output = ExchangeBuilder.anExchange(getCamelContext())
					.withBody(returned)
					.build();
			return output;
		}
		
	}
	
	@RestClient LibrariesIOClient librariesIo;
    @Override
    public void configure() throws Exception {
    	from("timer:autostart?repeatCount=1")
    		.routeId("get-all-platforms")
    		.description("Get all libraries platforms from libraries.io")
    		.process(this::getPlatforms)
    		.split(body(), new ReaggregateListsOfLibraries())
	    		.description("Split per deployment platform")
	    		.to("direct:get-all-libraries-of-platform")
	    		.end()
    		.setHeader("CamelFileName", constant("libraries.json"))
    		// I do prefer to have whitespaces
    		.marshal().json(true)
    		.to(String.format("file://%s", storageFolderPath))
	    	;
    	
    	from("direct:get-all-libraries-of-platform")
    		.routeId("get-all-libraries-of-platform")
    		.description("Get the first n libraries for the given platform from libraries.io.\n"
    				+ "Notice this also downloads all details (versions and so on) from libraries to put them in cache ... somewhere")
    		.process(this::readTheLibrariesForTheGivenPlatform)
    		.split(body(), AggregationStrategies.groupedBody())
	    		.description("Split per library")
	    		.to("direct:extract-library-infos")
	    		.end()
    		.to("log:get-all-libraries-of-platform")
	    	;
    	
    	from("direct:extract-library-infos")
    		.description("Saves a JSON file containing library infos then export the library GUID")
//    		.multicast(AggregationStrategies.groupedBody())
//    			.to("direct:save-library-infos")
    			.to("direct:get-library-guid")
    		.end()
    		;
    	
    	from("direct:get-library-guid")
    		.setBody(simple("${body.packageManagerUrl}"))
    		.choice()
    		// This is not good since we filter out dependencies of some
    		// package managers (swift and so on)
    		  .when(body().isEqualTo(null))
    		   .stop()
    		.end()
    		;
    	from("direct:save-library-infos")
    		// TODO find a way to use csimple (which won't run interpreter at runtime but rather compile some code)
    		.setHeader("CamelFileName", simple("libraries/${body.platform}/${body.name}.json"))
    		// I do prefer to have whitespaces
    		.marshal().json(true)
    		.to(String.format("file://%s", storageFolderPath))
    		// Once file is written, we no more need it
    		.stop()
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