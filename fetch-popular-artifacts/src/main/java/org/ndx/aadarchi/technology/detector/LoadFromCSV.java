package org.ndx.aadarchi.technology.detector;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

public class LoadFromCSV extends EndpointRouteBuilder {

	@Override
	public void configure() throws Exception {
		from(direct(getClass().getSimpleName()))
			.id("1-start-loading-from-csv")
			.description("Load as much tables as possible from CSV files using Camel")
			.log("Loading data from CSV")
			;
	}

}
