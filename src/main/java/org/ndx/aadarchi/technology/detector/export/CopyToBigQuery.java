package org.ndx.aadarchi.technology.detector.export;

import java.util.List;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CopyToBigQuery extends EndpointRouteBuilder  {
	
	@Override
	public void configure() throws Exception {
		String serializeTechnologies = getClass().getSimpleName()+"-serialize-technologies";
		String serializeIndicators = getClass().getSimpleName()+"-serialize-indicators";
		String serializeStars = getClass().getSimpleName()+"-serialize-stars";
		String serializeForks = getClass().getSimpleName()+"-serialize-forks";
		from(direct(getClass().getSimpleName()))
			.to(direct(serializeTechnologies), 
					direct(serializeIndicators),
					direct(serializeStars),
					direct(serializeForks)
					)
			;
		from(direct(serializeTechnologies))
			.id(getClass().getSimpleName()+"-1-serialize-technologies")
			.to(sql("select * from technology").dataSource("#tech-trendz"))
			.to(googleBigquery("tendances-tech-et-opportunites:aadarchi_technology_detector:technology")
					)
			.log(LoggingLevel.INFO, "Technologies have been sent to BigQuery!");
		from(direct(serializeIndicators))
			.id(getClass().getSimpleName()+"-2-serialize-indicators")
			.to(sql("select * from indicator")
					.dataSource("#tech-trendz")
					.outputType("StreamList"))
			// Seems like a bad case of interrelation between
			// https://camel.apache.org/components/4.10.x/sql-component.html#_using_streamlist
			// and 
			// https://camel.apache.org/components/4.10.x/google-bigquery-component.html#_producer_endpoints
		    .split(body()).streaming()
				.to(googleBigquery("tendances-tech-et-opportunites:aadarchi_technology_detector:indicator")
						)
			.end()
			.end()
			.log(LoggingLevel.INFO, "Indicators have been sent to BigQuery!");
		from(direct(serializeStars))
			.id(getClass().getSimpleName()+"-3-serialize-stars")
			.to(sql("select * from github_stars").dataSource("#tech-trendz"))
			.split().body()
			.to(googleBigquery("tendances-tech-et-opportunites:aadarchi_technology_detector:github_stars")
					)
			.end()
			.log(LoggingLevel.INFO, "GitHub Stars have been sent to BigQuery!");
		from(direct(serializeForks))
			.id(getClass().getSimpleName()+"-3-serialize-forks")
			.to(sql("select * from github_forks").dataSource("#tech-trendz"))
			.split().body()
			.to(googleBigquery("tendances-tech-et-opportunites:aadarchi_technology_detector:github_forks")
					)
			.end()
			.log(LoggingLevel.INFO, "GitHub Forks have been sent to BigQuery!");
	}
}
