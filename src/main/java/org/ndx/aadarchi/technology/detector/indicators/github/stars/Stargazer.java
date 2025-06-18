package org.ndx.aadarchi.technology.detector.indicators.github.stars;

import java.io.Serializable;
import java.util.Date;

import org.ndx.aadarchi.technology.detector.export.bigquery.annotations.BigQueryTable;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@BigQueryTable
@Entity
@Table(name = "GITHUB_STARS")
public class Stargazer extends PanacheEntityBase {
	@Embeddable
	public static class StargazerId implements Serializable {
	    @Column(name = "REPO_OWNER") public String owner;
	    @Column(name = "REPO_NAME") public String repo;
	    @Column(name = "STAR_DATE") public Date date;
	    @Column(name = "STAR_USER") public String user;
	    
	}
	
	@EmbeddedId
	public StargazerId id;
	
	public Stargazer() {
		super();
	}

	public Stargazer(String owner, String repo, Date date, String user) {
		this.id = new StargazerId();
		this.id.owner = owner;
		this.id.repo = repo;
		this.id.date = date;
		this.id.user = user;
	}
}
