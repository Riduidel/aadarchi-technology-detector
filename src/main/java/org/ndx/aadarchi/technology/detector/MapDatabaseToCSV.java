package org.ndx.aadarchi.technology.detector;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.builder.EndpointConsumerBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.mapping.Collection;
import org.hibernate.type.SqlTypes;
import org.ndx.aadarchi.technology.detector.indicators.github.stars.GitHubStars;
import org.ndx.aadarchi.technology.detector.indicators.github.stars.Stargazer;
import org.ndx.aadarchi.technology.detector.model.Indicator;
import org.ndx.aadarchi.technology.detector.model.Technology;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

@ApplicationScoped
public class MapDatabaseToCSV extends EndpointRouteBuilder {
	public static final String WRITE_TO_CSV_ROUTE = MapDatabaseToCSV.class.getSimpleName()+"_write";
	public static final String READ_FROM_CSV_ROUTE = MapDatabaseToCSV.class.getSimpleName()+"_read";

	@ConfigProperty(name = "tech-trends.csv.folder", defaultValue = ".cache/csv")
	public Path csvBaseFolder;
	
	@Inject EntityManager entityManager;
	
	@Override
	@ActivateRequestContext
	public void configure() throws Exception {
		// TODO make all that dynamic
		// The following code quite works, excepted it may not load
		// elements in the right order, which could lead to integrity constraints
		// not validated
//		Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();
//		 Set<ClassPersister> classes = entities.stream()
//				.sorted(new ByDependenciesComparator())
//				.map(EntityType::getBindableJavaType)
//				// TODO make that automatic
//				.filter(c -> c.getPackageName().startsWith("org.ndx.aadarchi"))
//				.map(ClassPersister::new)
//				.collect(Collectors.toSet())
//				;
		Set<ClassPersister> classes = Arrays.asList(
				Technology.class
//				Indicator.class,
//				Stargazer.class
				).stream()
				.map(ClassPersister::new)
				.collect(Collectors.toSet());
				
		configureRead(classes);
		configureWrite(classes);
	}

	private void configureRead(Set<ClassPersister> entities) {
		List<String> loaderRoutes = entities.stream()
				.map(this::generateReaderRouteFor)
				.collect(Collectors.toList());
			from(direct(READ_FROM_CSV_ROUTE))
				.id("1-start-reading-from-csv")
				.description("Load as much tables as possible from CSV files using Camel")
				.log("Reading all data from CSV files in "+csvBaseFolder)
				.multicast().recipientList(constant(loaderRoutes)).parallelProcessing()
				.end()
				.log("All data have been read, we can safely start")
				;
	}

	private void configureWrite(Set<ClassPersister> entities) {
		List<String> persisterRoutes = entities.stream()
			.map(this::generateWriterRouteFor)
			.collect(Collectors.toList());
		from(direct(WRITE_TO_CSV_ROUTE))
			.id("1-start-saving-to-csv")
			.description("Save as much tables as possible to CSV files using Camel")
			.log("Writing all datav to CSV files in "+csvBaseFolder)
			.multicast().recipientList(constant(persisterRoutes)).parallelProcessing()
			.end()
			.log("All data have been written, we can safely stop")
			;
	}
	
	private class ClassPersister {

		private String name;
		private String generatedSelect;
		private String writerRouteName;
		private String fileName;
		private String folderPath;
		private Class<?> clazz;
		private String readerRouteName;
		private String filePath;

		public ClassPersister(Class<?> clazz) {
			this.clazz = clazz; 
			name = clazz.getSimpleName();
			generatedSelect = constructSelect(clazz);
			writerRouteName = String.format("%s-%s", WRITE_TO_CSV_ROUTE, name);
			readerRouteName = String.format("%s-%s", READ_FROM_CSV_ROUTE, name);
			fileName = String.format("%s.csv", extractSQLTableName(clazz).toLowerCase());
			folderPath = String.format("%s?charset=UTF-8&delete=false&noop=true", 
					csvBaseFolder.toUri().toString());
			filePath = String.format("%s&fileName=%s", folderPath, fileName);
		}
		
	}

	private String generateReaderRouteFor(ClassPersister persister) {
		EndpointConsumerBuilder routePath = direct(persister.readerRouteName);
		from(routePath)
			.id(persister.readerRouteName)
			.description(String.format("Persist all instances of %s to %s", 
					persister.name, 
					persister.fileName))
			.log(String.format("Reading all instances of %s from CSV file", persister.name))
			.pollEnrich(persister.filePath)
			.unmarshal().csv()
			.log("${body}")
			.to("jdbc:default")
			.log(String.format("Written all instances of %s using JDBC", persister.name))
			;
		return routePath.getUri();
	}


	private String generateWriterRouteFor(ClassPersister persister) {
		EndpointConsumerBuilder routePath = direct(persister.writerRouteName);
		from(routePath)
			.id(persister.writerRouteName)
			.description(String.format("Persist all instances of %s to %s", 
					persister.name, 
					persister.fileName))
			.setBody(constant(persister.generatedSelect))
			.log(String.format("Reading all instances of %s using JDBC", persister.name))
			.to("jdbc:default")
			// TODO add header writing
			.marshal().csv()
			.to(persister.filePath)
			.log("Written data to "+persister.fileName)
			;
		return routePath.getUri();
	}

	private String constructSelect(Class clazz) {
		return String.format("SELECT %s FROM %s", 
				getColumnsForFieldsOf(clazz), 
				extractSQLTableName(clazz));
	}

	private String getColumnsForFieldsOf(Class clazz) {
		return Arrays.stream(clazz.getDeclaredFields())
			.filter(field -> !Modifier.isStatic(field.getModifiers()))
			.filter(field -> !field.getName().startsWith("$$"))
			.map(this::extractSQLField)
			.collect(Collectors.joining(", "));
	}

	private String extractSQLTableName(Class<?> class1) {
		Entity tableName = class1.getAnnotation(Entity.class);
		if(tableName!=null) {
			if(tableName.name()!=null && !tableName.name().isBlank()) {
				return tableName.name();
			}
		}
		return class1.getSimpleName().toUpperCase();
	}

	private String extractSQLField(Field f) {
		EmbeddedId e = f.getAnnotation(EmbeddedId.class);
		if(e==null) {
			Column c = f.getAnnotation(Column.class);
			String columnName = f.getName().toUpperCase();
			if(c!=null) {
				if(c.name()!=null && !c.name().isBlank()) {
					columnName = c.name();
				}
			}
			JdbcTypeCode jdbcType = f.getAnnotation(JdbcTypeCode.class);
			if(jdbcType!=null) {
				switch(jdbcType.value()) {
				case SqlTypes.JSON:
					return String.format("UTF8TOSTRING(%s)", columnName);
				default:
					return columnName;
				}
			}
			// There is a final subtelty: what if data type is collection?
			// Then we must transform the h2 array into something more convenient...
			return columnName;
		} else {
			// This is an embedded id, extract the class fields
			return getColumnsForFieldsOf(f.getType());
		}
	}
}
