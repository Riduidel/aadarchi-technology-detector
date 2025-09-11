package org.ndx.aadarchi.technology.detector.indicators.stackoverflow;

import org.ndx.aadarchi.technology.detector.model.Indicator;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

/**
 * The tags repository tries to maintain an updated database of Stackoverflow, 
 * complete with wiki pages (which allow us to link technologies to tags) 
 */
@ApplicationScoped
public class TagsRepository implements PanacheRepository<Tag> {
		private final EntityManager entityManager;

		public TagsRepository(EntityManager entityManager) {
			this.entityManager = entityManager;
		}

}
