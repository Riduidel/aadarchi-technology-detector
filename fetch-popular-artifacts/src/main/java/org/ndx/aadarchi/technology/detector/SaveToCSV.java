package org.ndx.aadarchi.technology.detector;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

public class SaveToCSV extends EndpointRouteBuilder {

	@Override
	public void configure() throws Exception {
		from(direct(getClass().getSimpleName()))
			.id("1-start-saving-to-csv")
			.description("Save as much tables as possible to CSV files using Camel")
			.log("Writing data to CSV")
			;
		
	}

}
