package org.ndx.aadarchi.technology.detector.export.json;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.dataformat.ParquetAvroDataFormat;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ndx.aadarchi.technology.detector.model.export.ComputedIndicators;
import org.ndx.aadarchi.technology.detector.processors.TechnologyRepositoryProcessor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ExportToJson extends EndpointRouteBuilder {
	public static final String READ_FROM_CSV_ROUTE = ExportToJson.class.getSimpleName()+"_read";
	TechnologyRepositoryProcessor technologies;
	
	@Inject
	public void setTechnologies(TechnologyRepositoryProcessor technologies) {
		this.technologies = technologies;
	}
	
	@ConfigProperty(name = "tech-lab-ingester.export.folder", defaultValue = "data/export")
	public Path exportBaseFolder;
	
	@Inject EntityManager entityManager;

	@Override
	public void configure() throws Exception {
		DirectEndpointBuilder exportToJson = direct(getClass().getSimpleName()+"-export-to-json");
		DirectEndpointBuilder exportToParquet = direct(getClass().getSimpleName()+"-export-to-parquet");
		from(direct(getClass().getSimpleName()))
			.id(getClass().getSimpleName()+"-1-export-all-database-informations")
			.setHeader("exportBaseFolder", () -> exportBaseFolder.toUri().toString())
			// Get all technologies
			// Add all indicators for each technology
			.log("🔍 Searching for technologies")
			// Load all technologies
			// I think it will be necessary to have some kind of batch processing
			.process(technologies::findAllTechnologies)
			.log("✅ Found ${body.size} technologies")
			.split(body(), AggregationStrategies.flexible(ComputedIndicators.class)
				    .accumulateInCollection(ArrayList.class)
				    .pick(body()))
				.parallelProcessing()
				.process(technologies::toComputedIndicators)
				.log(LoggingLevel.DEBUG, "Technology ${header.CamelSplitIndex}/${header.CamelSplitSize} ${body.technology.name} indicators have been aggregated")
			.end()
			.recipientList(constant(exportToJson /*, exportToParquet */))
			;
		from(exportToJson)
			.setHeader("exportJson", simple("${header.exportBaseFolder}?charset=utf-8&noop=true&directoryMustExist=false&filename=export.json"))
			.marshal().json(JsonLibrary.Jackson, true)
			.log("⌛ Exporting to ${header.exportJson}")
			.toD("${header.exportJson}")
			.log("🎉 Exported to ${header.exportJson}")
			.end()
			;
		// Thanks https://github.com/apache/camel/pull/10576/files#diff-7454d0ee361c52f7f30ae0924618f456969606f52303b6baf9274cf04cd2d20bR145
//		ParquetAvroDataFormat parquet = new ParquetAvroDataFormat();
//		parquet.setCompressionCodecName(CompressionCodecName.GZIP.name());
//		parquet.setUnmarshalType(ComputedIndicators.class);
//		from(exportToParquet)
//			.setHeader("exportParquet", simple("${header.exportBaseFolder}?charset=utf-8&noop=true&directoryMustExist=false&filename=export.parquet"))
//			.marshal(parquet)
//			.log("Exporting to ${header.exportParquet}")
//			.toD("${header.exportParquet}")
//			.log("Exported to ${header.exportParquet}")
//			.end()
//			;
	}
}
