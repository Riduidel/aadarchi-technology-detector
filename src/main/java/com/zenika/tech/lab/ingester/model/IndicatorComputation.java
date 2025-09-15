package com.zenika.tech.lab.ingester.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "INDICATOR_COMPUTATION")
public class IndicatorComputation extends PanacheEntityBase {
	@Embeddable
	public static class IndicatorComputationId implements Serializable {
	    @ManyToOne
	    @JoinColumn(name = "TECHNOLOGY_ID",insertable = false, updatable = false, foreignKey = @ForeignKey(name="fk_technology_id"))
		public Technology technology;
		@Column(name = "INDICATOR_ROUTE")
		public String indicatorRoute;
		@Override
		public int hashCode() {
			return Objects.hash(indicatorRoute, technology);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IndicatorComputationId other = (IndicatorComputationId) obj;
			return Objects.equals(indicatorRoute, other.indicatorRoute)
					&& Objects.equals(technology, other.technology);
		}
		@Override
		public String toString() {
			return String.format("IndicatorComputationId [technology=%s, indicatorRoute=%s]", 
					technology,
					indicatorRoute);
		}
	}
	
	public static enum IndicatorComputationStatus {
		/** Status of an indicator computation stored in db, but not present in any query */
		HOLD,
		/** Status of an indicator computation loaded in memory */
		LOADED
	}
	
	@EmbeddedId
	public IndicatorComputationId id;

	@Column(name = "INDICATOR_DATE")
	public Date date;
	
	@Column(name = "STATUS")
	public IndicatorComputationStatus status;
	
	public IndicatorComputation() {}
	
	public IndicatorComputation(Technology technology, String indicatorRoute) {
		this();
		id = new IndicatorComputationId();
		id.technology = technology;
		id.indicatorRoute = indicatorRoute;
		// All indicator computations are initialized at EPOCH, to force their recomputation
		date = new Date(0);
		status = IndicatorComputationStatus.HOLD;
	}

	@Override
	public String toString() {
		return String.format("IndicatorComputation [status=%s, id=%s, date=%s]", status, id, date);
	}

}
