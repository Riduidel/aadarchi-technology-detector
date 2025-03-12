package org.ndx.aadarchi.technology.detector.indicators.github;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import org.ndx.aadarchi.technology.detector.model.Technology;
import org.kohsuke.github.GHStargazer;
import org.ndx.aadarchi.technology.detector.model.Indicator.IndicatorId;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "GITHUB_STARS")
public class Stargazer extends PanacheEntityBase {
	@Embeddable
	public static class StargazerId implements Serializable {
	    @ManyToOne
	    @JoinColumn(name = "technology",insertable = false, updatable = false)
		public Technology technology;
	    @Column
		public Date date;
	    
	}
	
	@EmbeddedId
	public StargazerId id;

	public Stargazer() {
		super();
	}

	public Stargazer(Technology technology, GHStargazer stargazer) {
		this.id = new StargazerId();
		this.id.technology = technology;
		this.id.date = stargazer.getStarredAt();
		Objects.requireNonNull(this.id.technology);
		Objects.requireNonNull(this.id.date);
	}

}
