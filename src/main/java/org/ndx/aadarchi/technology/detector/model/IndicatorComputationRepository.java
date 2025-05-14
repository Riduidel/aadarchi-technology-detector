package org.ndx.aadarchi.technology.detector.model;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IndicatorComputationRepository implements PanacheRepository<IndicatorComputation>{
	@Transactional
	public IndicatorComputation findOrCreateFromTechnology(Technology body, String indicatorRoute) {
		IndicatorComputation returned = null;
		// First find the reference url
		if(returned==null && body.id!=null) {
			returned = find("id.technology=?1 and id.indicatorRoute=?2", body, indicatorRoute).firstResult();
			if(returned!=null) {
				return returned;
			}
		}
		// If not found, create it and persist it immediatly
		returned = new IndicatorComputation(body, indicatorRoute);
		persist(returned);
		return returned;
	}
}
