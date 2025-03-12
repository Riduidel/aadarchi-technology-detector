package org.ndx.aadarchi.technology.detector.indicators.github;

import org.kohsuke.github.GHStargazer;
import org.ndx.aadarchi.technology.detector.model.Indicator;
import org.ndx.aadarchi.technology.detector.model.Technology;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class StargazerRepository implements PanacheRepository<Stargazer> {

	@Transactional
	public void persist(Technology technology, GHStargazer stargazer) {
		Stargazer persistent = new Stargazer(technology, stargazer);
		if(count("id.technology = ?1 and id.date = ?2", technology, stargazer.getStarredAt())==0) {
			persistent.persist();
		}
	}

}
