package org.ndx.aadarchi.technology.detector;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.ndx.aadarchi.technology.detector.export.CopyToBigQuery;
import org.ndx.aadarchi.technology.detector.export.ExportToFile;

public class StarterRoute extends EndpointRouteBuilder {
	
	@Override
	public void configure() throws Exception {
		from(timer("autostart").repeatCount(1))
			.id("1-starter-route")
			.log("Starting the whole process")
			.to(direct(ReadPopularLibraries.class.getSimpleName()))
			.to(direct(GenerateIndicatorComputations.class.getSimpleName()))
			.to(direct(ProcessIndicatorComputations.class.getSimpleName()))
			.to(direct(ExportToFile.class.getSimpleName()))
//			.to(direct(CopyToBigQuery.class.getSimpleName()))
			.log("Everything should be terminated now.")
			;
	}

}
