package org.ndx.aadarchi.technology.detector;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.SedaEndpointBuilderFactory.SedaEndpointBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ndx.aadarchi.technology.detector.indicators.github.stars.Stargazer;
import org.ndx.aadarchi.technology.detector.model.Indicator;
import org.ndx.aadarchi.technology.detector.model.Technology;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@ApplicationScoped
public class SaveToCSV extends EndpointRouteBuilder {
	@ConfigProperty(name = "tech-rends.csv.folder", defaultValue = "src/main/resources/csv")
	public Path csvBaseFolder;
	
	@Override
	public void configure() throws Exception {
		from(direct(getClass().getSimpleName()))
			.id("1-start-saving-to-csv")
			.description("Save as much tables as possible to CSV files using Camel")
			.to(generateRouteFor(Technology.class))
			.to(generateRouteFor(Indicator.class))
			.to(generateRouteFor(Stargazer.class))
			;
	}

	private SedaEndpointBuilder generateRouteFor(Class<?> clazz) {
		String name = clazz.getSimpleName();
		String generatedSelect = constructSelect(clazz);
		String routeName = String.format("persist-%s", name);
		SedaEndpointBuilder routePath = seda(routeName);
		String fileName = String.format("%s.csv", extractSQLTableName(clazz).toLowerCase());
		String filePath = String.format("%s?charset=UTF-8", 
				csvBaseFolder.toUri().toString());
		from(routePath)
			.setBody(constant(generatedSelect))
			.log("Sending JDBC extraction request")
			.to("jdbc:default")
			.marshal().csv()
			.setHeader("CamelFileName", constant(fileName))
			.to(filePath)
			.log("Written data to ${header.CamelFileName}")
			.stop()
			;
		return routePath;
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
			return columnName;
		} else {
			// This is an embedded id, extract the class fields
			return getColumnsForFieldsOf(f.getType());
		}
	}
}
