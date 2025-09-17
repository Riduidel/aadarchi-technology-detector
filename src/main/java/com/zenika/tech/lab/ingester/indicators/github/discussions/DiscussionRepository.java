package com.zenika.tech.lab.ingester.indicators.github.discussions;

import com.zenika.tech.lab.ingester.indicators.github.GithubIndicatorRepository;
import com.zenika.tech.lab.ingester.model.Indicator;
import com.zenika.tech.lab.ingester.model.Technology;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.apache.camel.util.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class DiscussionRepository implements GithubIndicatorRepository<Discussion>, PanacheRepository<Discussion>{

    @ConfigProperty(name = "tech-lab-ingester.indicators.github.discussions.sql.indicator")
    String groupDiscussionsByMonthsSql;
    private final EntityManager entityManager;

    public DiscussionRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Persists a Discussion event if it doesn't already exist.
     * @param persistent The Discussion entity to persist.
     * @return true if the entity was persisted (didn't exist), false otherwise.
     */
    @Transactional
    public boolean maybePersist(Discussion persistent) {
        if (count("id.owner = ?1 and id.repo = ?2 and id.date = ?3 and id.user = ?4",
        		persistent.id.owner,
                persistent.id.repo,
                persistent.id.date,
                persistent.id.user
        ) == 0) {
            persistent.persist();
            Log.debugf("Persisted new Discussion: %s/%s by %s",
                    persistent.id.owner, persistent.id.repo, persistent.id.user);
            return true;
        } else {
            Log.tracef("Discussion already exists, skipping persistence: %s/%s by %s",
                    persistent.id.owner, persistent.id.repo, persistent.id.user);
            return false;
        }
    }

    /**
     * Counts the total number of locally registered discussions for a given repository.
     * @param path Pair containing the owner (left) and repository name (right).
     * @return The number of local discussions for this repository.
     */
    @Transactional
    public long count(Pair<String> path) {
        return count("id.owner = ?1 and id.repo = ?2",
                path.getLeft(),
                path.getRight()
        );
    }

    /**
     * Groups discussions by month and year using a native SQL query.
     * @param technology The technology associated with these indicators.
     * @param pair Pair containing the repository owner and name.
     * @return A list of Indicator objects representing the number of discussions per month.
     */
    @Transactional
    public List<Indicator> groupIndicatorsByMonths(Technology technology, Pair<String> pair) {
        Query extractionQuery = entityManager.createNativeQuery(groupDiscussionsByMonthsSql);
        extractionQuery.setParameter("owner", pair.getLeft());
        extractionQuery.setParameter("name", pair.getRight());
        List<Object[]> results = extractionQuery.getResultList();
        return results.stream()
                .map(row -> toDiscussionIndicator(technology, row))
                .toList();
    }

    /**
     * Converts a native SQL query result row into an Indicator object.
     * Expects the row to contain [Year, Month, Count].
     * @param technology The associated technology.
     * @param row An array of objects representing a result row.
     * @return An Indicator object.
     */
    private Indicator toDiscussionIndicator(Technology technology, Object[] row) {
        LocalDate localDate = LocalDate.of(Integer.parseInt(row[0].toString()),
                Integer.parseInt(row[1].toString()), 1);
        Date d = Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
        return new Indicator(
                technology,
                GitHubDiscussionsIndicatorComputer.GITHUB_DISCUSSIONS,
                d,
                row[2].toString()
        );
    }
}
