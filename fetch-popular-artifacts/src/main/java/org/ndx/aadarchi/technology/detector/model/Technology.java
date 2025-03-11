package org.ndx.aadarchi.technology.detector.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ndx.aadarchi.technology.detector.librariesio.model.Project;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Class containing stored informations of technologies.
 */
@Entity
public class Technology extends PanacheEntityBase {
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = true)
	public Optional<Project> librariesIoProject;
	
	public String name;
	@Column(columnDefinition = "text")
	public String description;

	/**
	 * The homepage is usually the technology vanity url
	 */
	public String homepage;
	public List<String> keywords;
	
	/**
	 * The reference url is to be used as an identifier for the whole model.
	 * Obviously, it should NEVER be null.
	 */
	@Id
	public String referenceUrl;
	/**
	 * When possible, this can be used as the reference
	 */
	public String packageManagerUrl;
	public String repositoryUrl;
	@Override
	public String toString() {
		return "Technology [" + (name != null ? "name=" + name + ", " : "")
				+ (homepage != null ? "homepage=" + homepage : "") + "]";
	}
	@Override
	public int hashCode() {
		return Objects.hash(referenceUrl);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Technology other = (Technology) obj;
		return Objects.equals(referenceUrl, other.referenceUrl);
	}
}
