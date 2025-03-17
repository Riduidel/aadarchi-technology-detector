package org.ndx.aadarchi.technology.detector;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

public class StarterRoute extends EndpointRouteBuilder {

	@Override
	public void configure() throws Exception {
		from(timer("autostart").repeatCount(1))
			.id("1-starter-route")
//			.to(direct(ReadPopularLibraries.class.getSimpleName()))
			.to(direct(GenerateCurrentIndicatorValues.class.getSimpleName()));
	}

}
