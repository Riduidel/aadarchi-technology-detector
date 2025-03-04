package org.ndx.aadarchi.technology.detector;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;

import io.github.ncasaux.camelplantuml.processor.GetRoutesInfoProcessor;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CamelPlantUML extends RouteBuilder {

	@Override
	public void configure() throws Exception {
        rest("camel-plantuml")
	        .get("diagram.puml")
	            .param().name("connectRoutes").type(RestParamType.query).defaultValue("false").endParam()
	            .to("direct:camel-plantuml-generate-plantuml")
	    ;

		from("direct:camel-plantuml-generate-plantuml").routeId("camel-plantuml-http-trigger")
		        .process(new GetRoutesInfoProcessor())
		;
	}

}
