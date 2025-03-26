package org.ndx.aadarchi.technology.detector;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

public class StarterRoute extends EndpointRouteBuilder {
	
	@Override
	public void configure() throws Exception {
		from(timer("autostart").repeatCount(1))
			.id("1-starter-route")
			.log("Starting the whole process")
			.to(direct(MapDatabaseToCSV.READ_FROM_CSV_ROUTE))
			.to(direct(ReadPopularLibraries.class.getSimpleName()))
			.to(direct(GenerateCurrentIndicatorValues.class.getSimpleName()))
			.to(direct(MapDatabaseToCSV.WRITE_TO_CSV_ROUTE))
			.log("Everything should be terminated now.")
			;
	}

}
