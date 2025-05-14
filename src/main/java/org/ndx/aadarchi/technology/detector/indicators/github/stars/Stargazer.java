package org.ndx.aadarchi.technology.detector.indicators.github.stars;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Date;

import org.ndx.aadarchi.technology.detector.model.Technology;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "GITHUB_STARS")
public class Stargazer extends PanacheEntityBase {
	@Embeddable
	public static class StargazerId implements Serializable {
	    @ManyToOne
	    @JoinColumn(name = "TECHNOLOGY_ID",insertable = false, updatable = false, foreignKey = @ForeignKey(name="fk_technology_id"))
		public Technology technology;
	    @Column(name = "STAR_USER") public String user;
	    @Column(name = "STAR_DATE") public Date date;
	    
	}
	
	@EmbeddedId
	public StargazerId id;
	
	public Stargazer() {
		super();
	}

	public Stargazer(Technology technology, Date date, String user) {
		this.id = new StargazerId();
		this.id.technology = technology;
		this.id.date = date;
		this.id.user = user;
	}
}
