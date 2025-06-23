package org.ndx.aadarchi.technology.detector.export.csv;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;
import org.apache.camel.builder.endpoint.dsl.FileEndpointBuilderFactory.FileEndpointBuilder;
import org.apache.camel.component.file.GenericFileExist;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ndx.aadarchi.technology.detector.StarterRoute;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;

@ApplicationScoped
public class ExportToCsv extends EndpointRouteBuilder {
	@Inject
	EntityManagerFactory entityManagerFactory;
	@ConfigProperty(name = "tech-trends.export.folder", defaultValue = "data/export")
	public Path exportBaseFolder;

	public static record CSVTableExport(String table, String readTable) {
		public String route() {
			return ExportToCsv.class.getSimpleName()+"-"+table;
		}
	}

	@Override
	public void configure() throws Exception {
		List<CSVTableExport> TABLE_DEFINITIONS = createTableDefinitions();
		List<DirectEndpointBuilder> routes = TABLE_DEFINITIONS.stream()
				.map(t -> t.route())
				.map(t -> direct(t))
				.collect(Collectors.toList());

		configureTableExports(TABLE_DEFINITIONS);
		from(direct(getClass().getSimpleName()))
			.id(getClass().getSimpleName())
			.recipientList(constant(routes))
			.log(LoggingLevel.INFO, "🎉 EVERYTHING have been exported to CSV! CELEBRATE!");
	}

	private void configureTableExports(List<CSVTableExport> tableDefinitions) {
		tableDefinitions.stream()
			.forEach(this::configureTableExportFor);
	}
	
	private void configureTableExportFor(CSVTableExport export) {
		// This generates the file:// prefix, so no need to add it afterwards
		String FILE_NAME = export.table.toUpperCase()+".csv";
		FileEndpointBuilder exportPath = file(exportBaseFolder.toFile().getPath())
				.charset("utf-8")
				.fileName(FILE_NAME);
		CsvDataFormat format = new CsvDataFormat();
		format.setIgnoreEmptyLines("true");
		format.setIgnoreSurroundingSpaces("true");
		format.setQuoteMode("NON_NUMERIC");
		format.setTrim("true");

		from(direct(export.route()+"-marshal-headers"))
			.id(export.route()+".headers")
			.process(this::putJdbcColumnNamesInBody)
			.marshal(format)
			.to(exportPath.fileExist(GenericFileExist.Override))
			.end();
		from(direct(export.route()+"-marshal-rows"))
			.id(export.route()+".rows")
			.marshal(format)
			.to(exportPath.fileExist(GenericFileExist.Append))
			.end();
		
		String EXPORT = "tech-trends.export."+export.table+".csv";
		String GLOBALLY_ENABLED = EXPORT+".enabled";
		String GCP_ENABLED = EXPORT+".to.gcp.enabled";
		
		from(direct(export.route()))
			.id(export.route())
			.choice()
			.when(simple("{{"+GLOBALLY_ENABLED+":true}}"))
				.process(exchange-> exportBaseFolder.resolve(FILE_NAME).toFile().delete())
				.log(LoggingLevel.INFO, "⌛️ Exporting "+export.table+" to CSV")
				.setBody(constant(export.readTable()))
				.to(jdbc("default").outputType("StreamList"))
				.multicast()
					.to(direct(export.route()+"-marshal-headers"))
					.to(direct(export.route()+"-marshal-rows"))
					.choice()
					.when(simple("{{"+GCP_ENABLED+":true}}"))
						.log(LoggingLevel.INFO, "📤 Pushing "+export.table+" to GCP")
						.pollEnrich(exportPath)
						.setHeader(GoogleCloudStorageConstants.OBJECT_NAME, constant(FILE_NAME))
						.to(googleStorage("aadarchi"))
					.endChoice()
					.otherwise()
						.log(LoggingLevel.WARN, "⛔ Export of "+export.table+" to GCP is disabled due to property "+GCP_ENABLED+" resolving to false")
					.end()
				.end()
				.log(LoggingLevel.INFO, "✅ Exported "+export.table+" to CSV (in "+exportBaseFolder+")")
				.endChoice()
			.otherwise()
				.log(LoggingLevel.WARN, "⛔ Export of "+export.table+" is disabled due to property "+GLOBALLY_ENABLED+" resolving to false")
			.end()
			;
	}
	
	private void putJdbcColumnNamesInBody(Exchange exchange) {
		Set<String> columnNames = (Set<String>) exchange.getMessage().getHeader("CamelJdbcColumnNames");
		ArrayList<Set<String>> body = new ArrayList<Set<String>>();
		body.add(columnNames);
		exchange.getMessage().setBody(body);
	}

	private List<CSVTableExport> createTableDefinitions() {
		String rootPackage = StarterRoute.class.getPackageName();
		Set<EntityType<?>> entities = entityManagerFactory.createEntityManager().getMetamodel().getEntities();
		return entities.stream()
			.map(e -> Map.entry(e, e.getBindableJavaType()))
			.filter(e -> e.getValue().isAnnotationPresent(Table.class) || e.getValue().isAnnotationPresent(jakarta.persistence.Entity.class))
			.filter(e -> e.getValue().getPackageName().startsWith(rootPackage))
			.map(e -> createTableExportForEntity(e.getKey(), e.getValue()))
			.sorted(Comparator.comparing(CSVTableExport::table).reversed())
			.collect(Collectors.toList());
	}

	public CSVTableExport createTableExportForEntity(EntityType<?> entity, Class<?> bindable) {
		final String TABLE_NAME = getEntityTableName(bindable);
		final String EXPORT_TO_CSV = TABLE_NAME+".CSV.EXPORT";
		final String DEFAULT_EXPORT_REQUEST = "select * from "+TABLE_NAME;
		Supplier<String> getDefaultExportRequest = () -> {
			Log.warnf("Using default request to export values of %s.\n"
					+ "To change that, create in %s a @%s(name=\"%s\", query=\"%s\")", 
					TABLE_NAME,
					bindable.getSimpleName(),
					NamedNativeQuery.class.getSimpleName(),
					EXPORT_TO_CSV,
					DEFAULT_EXPORT_REQUEST);
			return DEFAULT_EXPORT_REQUEST;
		};
		String exportRequest = null;
		if(bindable.isAnnotationPresent(NamedNativeQueries.class)) {
			NamedNativeQueries queries = bindable.getAnnotation(NamedNativeQueries.class);
			exportRequest = Arrays.stream(queries.value())
				.filter(query -> EXPORT_TO_CSV.equals(query.name()))
				.map(query -> query.query())
				.findFirst()
				.orElseGet(getDefaultExportRequest);
		} else {
			exportRequest = getDefaultExportRequest.get();
		}
		return new CSVTableExport(TABLE_NAME, exportRequest);
	}

	private String getEntityTableName(Class<?> bindable) {
		String name = null;
		if(bindable.isAnnotationPresent(Table.class)) {
			name = bindable.getAnnotation(Table.class).name();
		} else if(bindable.isAnnotationPresent(jakarta.persistence.Entity.class)) {
			name = bindable.getAnnotation(jakarta.persistence.Entity.class).name();
		}
		if(name==null|| name.isBlank()){
			name = snakeCase(bindable.getSimpleName());
		}
		return name.toUpperCase();
	}

	private String snakeCase(String text) {
		return text.replaceAll("([A-Z])(?=[A-Z])", "$1_")
		  .replaceAll("([a-z])([A-Z])", "$1_$2")
		  .toLowerCase();
	}
}
