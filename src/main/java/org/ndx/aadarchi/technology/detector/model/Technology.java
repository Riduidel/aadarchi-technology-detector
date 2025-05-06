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
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Class containing stored informations of technologies.
 */
@Entity
public class Technology extends PanacheEntityBase {
	public String name;
	@Column(columnDefinition = "text")
	public String description;

	/**
	 * The homepage is usually the technology vanity url
	 */
	public String homepage;

	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TECHNOLOGY_ID_SEQ")
	public Long id;
	public String packageManagerUrl;
	public String repositoryUrl;
	@Override
	public String toString() {
		return "Technology [" + (name != null ? "name=" + name + ", " : "")
				+ (homepage != null ? "homepage=" + homepage : "") + "]";
	}
	@Override
	public int hashCode() {
		return Objects.hash(id);
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
		return Objects.equals(id, other.id);
	}
}
