package org.ndx.aadarchi.technology.detector.export.bigquery;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;
import org.apache.camel.builder.endpoint.dsl.GoogleBigQuerySQLEndpointBuilderFactory.GoogleBigQuerySQLEndpointBuilder;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ndx.aadarchi.technology.detector.export.bigquery.annotations.BigQueryTable;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;

@ApplicationScoped
public class CopyToBigQuery extends EndpointRouteBuilder  {
	public static final String CONFIGURATION_BASE = "tech-trends.exporter.bigquery";

	@ConfigProperty(name = CONFIGURATION_BASE+".project.id", defaultValue = "10")
	private String bigQueryProjectId;
	@Inject
    EntityManagerFactory entityManagerFactory;
    
	@Override
	public void configure() throws Exception {
		List<BigQueryTableDefinition> TABLE_DEFINITIONS = createTableDefinitions();
		List<DirectEndpointBuilder> routes = TABLE_DEFINITIONS.stream()
			.map(t -> t.tablePath())
			.map(t -> direct(t))
			.collect(Collectors.toList());
		from(direct(getClass().getSimpleName()))
			.recipientList(constant(routes))
			.log(LoggingLevel.INFO, "EVERYTHING have been sent to BigQuery! CELEBRATE! 🎉");
			;
		configureTableExport(TABLE_DEFINITIONS);
	}

	private void configureTableExport(List<BigQueryTableDefinition> TABLE_DEFINITIONS) {
		for(BigQueryTableDefinition t : TABLE_DEFINITIONS) {
			if(t.readAsStream()) {
				from(direct(t.tablePath()))
				.log(LoggingLevel.INFO, "Sending all "+t.logMessage()+" to BigQuery")
				// But first, get latest from BigQuery
				// Oh the fuckery https://stackoverflow.com/a/52534289/15619
				.to(sql(t.readQuery().replaceAll("[\\n\\r]", " "))
						.dataSource("#tech-trendz")
						.outputType("StreamList"))
				// Seems like a bad case of interrelation between
				// https://camel.apache.org/components/4.10.x/sql-component.html#_using_streamlist
				// and 
				// https://camel.apache.org/components/4.10.x/google-bigquery-component.html#_producer_endpoints
			    .split(body()).streaming()
			    	// So we unfortunatly have to partition table
//			    	.setHeader(GoogleBigQueryConstants.PARTITION_DECORATOR, )
					.to(googleBigquery(t.tablePath()))
				.end()
				.log(LoggingLevel.INFO, "Sent all "+t.logMessage()+" to BigQuery!")
				.end();
			} else {
				from(direct(t.tablePath()))
				// Oh the fuckery https://stackoverflow.com/a/52534289/15619
				.to(sql(t.readQuery().replaceAll("[\\n\\r]", " "))
						.dataSource("#tech-trendz"))
				.to(googleBigquery(t.tablePath()))
				.log(LoggingLevel.INFO, "Sent all "+t.logMessage()+" to BigQuery!")
				.end();
			}
		}
	}

	private List<BigQueryTableDefinition> createTableDefinitions() {
		
		Set<EntityType<?>> entities = entityManagerFactory.createEntityManager().getMetamodel().getEntities();
		return entities.stream()
			.map(EntityType::getBindableJavaType)
			.filter(clazz -> clazz.isAnnotationPresent(BigQueryTable.class))
			.map(this::createTableDefinitionForEntity)
			.sorted(Comparator.comparing(BigQueryTableDefinition::tablePath).reversed())
			.collect(Collectors.toList());
	}
	
	private BigQueryTableDefinition createTableDefinitionForEntity(Class<?> bindable) {
		BigQueryTable bigQueryTable = bindable.getAnnotation(BigQueryTable.class);
		Optional<Table> jpaTable = Optional.ofNullable(bindable.isAnnotationPresent(Table.class) ?
				bindable.getAnnotation(Table.class) : null);
		String snakeName = jpaTable
				.map(t -> t.name().toLowerCase())
				.orElseGet(() -> 
					bindable.getSimpleName().replaceAll("([A-Z])(?=[A-Z])", "$1_")
				      .replaceAll("([a-z])([A-Z])", "$1_$2")
				      .toLowerCase()
				);
		String configurationPath = String.format("%s.%s", CONFIGURATION_BASE, snakeName);
		
		return new BigQueryTableDefinition(
				ConfigProvider.getConfig().getValue(configurationPath+".id", String.class),
				ConfigProvider.getConfig().getOptionalValue(configurationPath+".read_locally", String.class)
					.orElse("select * from "+jpaTable.map(t -> t.name()).orElse(bindable.getSimpleName())),
				ConfigProvider.getConfig().getOptionalValue(configurationPath+".find_latest_remote", String.class).orElse(""),
				ConfigProvider.getConfig().getOptionalValue(configurationPath+".log_message", String.class)
					.orElse(bindable.getSimpleName()),
				ConfigProvider.getConfig().getOptionalValue(configurationPath+".read_as_stream", Boolean.class).orElse(false)
				);
	}
}
