package org.ndx.aadarchi.technology.detector;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.ndx.aadarchi.technology.detector.export.csv.ExportToCsv;
import org.ndx.aadarchi.technology.detector.export.json.ExportToJson;

public class StarterRoute extends EndpointRouteBuilder {
	
	@Override
	public void configure() throws Exception {
		from(timer("autostart").repeatCount(1))
			.id("1-starter-route")
			.log("🚀 Starting the whole process")
//			.to(direct(ReadPopularLibraries.class.getSimpleName()))
			.to(direct(AddMissingFields.class.getSimpleName()))
//			.to(direct(GenerateIndicatorComputations.class.getSimpleName()))
//			.to(direct(ProcessIndicatorComputations.class.getSimpleName()))
//			.to(direct(ExportToJson.class.getSimpleName()))
//			.to(direct(ExportToCsv.class.getSimpleName()))
			.log("🏁 Everything should be terminated now.")
			;
	}

}
