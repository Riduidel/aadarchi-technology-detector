package org.ndx.aadarchi.technology.detector;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ndx.aadarchi.technology.detector.model.Technology;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@ApplicationScoped
public class SaveToCSV extends EndpointRouteBuilder {
	private static final String PERSIST_TECHNOLOGIES = "persist-technologies";
	@ConfigProperty(name = "tech-rends.csv.folder", defaultValue = "src/main/resources/csv")
	public Path csvBaseFolder;
	
	@Override
	public void configure() throws Exception {
		from(direct(getClass().getSimpleName()))
			.id("1-start-saving-to-csv")
			.description("Save as much tables as possible to CSV files using Camel")
			.to(direct(PERSIST_TECHNOLOGIES))
			;
		
		String technologiesSelect = constructSelect(Technology.class);
		from(direct(PERSIST_TECHNOLOGIES))
			.setBody(constant(technologiesSelect))
			.log("Sending JDBC extraction request")
			.to("jdbc:default")
			// Funnily (or not), this currently imples the LibrariesIO content will be persisted as a stupid byte array
			.log("Marshalling to CSV")
			.marshal().csv()
			.setHeader("CamelFileName", constant("technologies.csv"))
			.log("Writing content")
			.to(String.format("%s?charset=UTF-8", 
					csvBaseFolder.toUri().toString()))
			.log("Written technologies to ${header.CamelFileName}")
			.stop()
			;
	}

	private String constructSelect(Class clazz) {
		String table = extractSQLTableName(clazz);
		String fields = Arrays.stream(clazz.getDeclaredFields())
			.filter(field -> !Modifier.isStatic(field.getModifiers()))
			.filter(field -> !field.getName().startsWith("$$"))
			.map(this::extractSQLField)
			.collect(Collectors.joining(", "));
		return String.format("SELECT %s FROM %s", fields, table);
	}

	private String extractSQLTableName(Class<Technology> class1) {
		Table tableName = class1.getAnnotation(Table.class);
		if(tableName!=null) {
			if(tableName.name()!=null) {
				return tableName.name();
			}
		}
		return class1.getSimpleName().toUpperCase();
	}

	private String extractSQLField(Field f) {
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
		return columnName;
	}
}
