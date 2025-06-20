package org.ndx.aadarchi.technology.detector.model;

import java.util.Objects;

import org.apache.avro.reflect.AvroDoc;
import org.apache.avro.reflect.AvroIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;

/**
 * Class containing stored informations of technologies.
 */
@AvroDoc("A technology (see glossary). This class is mainly modeled from libraries.io representation")
@Entity
@NamedNativeQueries({
	@NamedNativeQuery(name="TECHNOLOGY.CSV.EXPORT", query="""
select id,
trim(regexp_replace(description, '[\n\r]+', ' ', 'g')) as description,
trim(regexp_replace(homepage, '[\n\r]+', ' ', 'g')) as homepage,
trim(regexp_replace(name, '[\n\r]+', ' ', 'g')) as name,
trim(regexp_replace(packagemanagerurl, '[\n\r]+', ' ', 'g')) as packagemanagerurl,
trim(regexp_replace(repositoryurl, '[\n\r]+', ' ', 'g')) as repositoryurl
from technology
			""")
})
public class Technology extends PanacheEntityBase {
	@AvroDoc("Common name of that technology")
	public String name;
	@AvroDoc("A longer description of technology")
	@Column(columnDefinition = "text")
	public String description;

	/**
	 * The homepage is usually the technology vanity url
	 */
	@AvroDoc("The homepage is usually the technology vanity url")
	public String homepage;

	@AvroIgnore
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TECHNOLOGY_ID_SEQ")
	public Long id;
	@AvroDoc("url of that technology in the package manager used to distribute it")
	public String packageManagerUrl;
	@AvroDoc("source code repository url")
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
