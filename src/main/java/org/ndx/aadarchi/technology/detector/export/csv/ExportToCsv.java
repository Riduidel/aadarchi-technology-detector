package org.ndx.aadarchi.technology.detector.export.csv;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ndx.aadarchi.technology.detector.StarterRoute;

import io.quarkus.hibernate.orm.runtime.dev.HibernateOrmDevInfo.Entity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
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

		from(direct(getClass().getSimpleName()))
			.recipientList(constant(routes))
			.log(LoggingLevel.INFO, "🎉 EVERYTHING have been exported to CSV! CELEBRATE!");
		configureTableExports(TABLE_DEFINITIONS);
	}

	private void configureTableExports(List<CSVTableExport> tableDefinitions) {
		tableDefinitions.stream()
			.forEach(this::configureTableExportFor);
	}
	
	private void configureTableExportFor(CSVTableExport export) {
		// This generates the file:// prefix, so no need to add it afterwards
		String exportPath = String.format("%s?"
				+ "?charset=utf-8&noop=true&directoryMustExist=false&filename=%s.csv", 
				exportBaseFolder.toUri().toString(),
				export.table);
		from(direct(export.route()))
			.log(LoggingLevel.INFO, "⌛️ Exporting "+export.table+" to CSV")
			.to(sql(export.readTable())
					.outputType("StreamList"))
			.marshal().csv()
			.to(exportPath)
			.log(LoggingLevel.INFO, "✅ Exported "+export.table+" to CSV (in "+exportBaseFolder+")")
			;
	}

	private List<CSVTableExport> createTableDefinitions() {
		String rootPackage = StarterRoute.class.getPackageName();
		Set<EntityType<?>> entities = entityManagerFactory.createEntityManager().getMetamodel().getEntities();
		return entities.stream()
			.map(EntityType::getBindableJavaType)
			.filter(clazz -> clazz.isAnnotationPresent(Table.class) || clazz.isAnnotationPresent(jakarta.persistence.Entity.class))
			.filter(clazz -> clazz.getPackageName().startsWith(rootPackage))
			.map(this::createTableExportForEntity)
			.sorted(Comparator.comparing(CSVTableExport::table).reversed())
			.collect(Collectors.toList());
	}

	public CSVTableExport createTableExportForEntity(Class<?> bindable) {
		String name = null;
		if(bindable.isAnnotationPresent(Table.class)) {
			name = bindable.getAnnotation(Table.class).name();
		} else if(bindable.isAnnotationPresent(jakarta.persistence.Entity.class)) {
			name = bindable.getAnnotation(jakarta.persistence.Entity.class).name();
		}
		if(name==null|| name.isBlank()){
			name = snakeCase(bindable.getSimpleName());
		}
		return new CSVTableExport(name, "select * from "+name);
	}

	private String snakeCase(String text) {
		return text.replaceAll("([A-Z])(?=[A-Z])", "$1_")
		  .replaceAll("([a-z])([A-Z])", "$1_$2")
		  .toLowerCase();
	}
}
