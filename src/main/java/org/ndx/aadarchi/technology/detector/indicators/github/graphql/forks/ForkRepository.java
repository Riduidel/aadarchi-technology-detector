package org.ndx.aadarchi.technology.detector.indicators.github.graphql.forks;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.logging.Log;
import org.apache.camel.util.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ndx.aadarchi.technology.detector.model.Indicator;
import org.ndx.aadarchi.technology.detector.model.Technology;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;


@ApplicationScoped
public class ForkRepository implements PanacheRepository<Fork>{

    @ConfigProperty(name = "tech-trends.indicators.github.forks.sql.indicator")
    public String groupForksByMonthsSql;
    private final EntityManager entityManager;

    public ForkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Persists a fork event if it doesn't already exist.
     * @param persistent The Fork entity to persist.
     * @return true if the entity was persisted (didn't exist), false otherwise.
     */
    @Transactional
    public boolean maybePersist(Fork persistent) {
        if (count("id.repoOwner = ?1 and id.repoName = ?2 and id.forkOwnerLogin = ?3",                persistent.id.repoOwner,
                persistent.id.repoName,
                persistent.id.forkedAt,
                persistent.id.forkOwnerLogin
        ) == 0) {
            persistent.persist();
            Log.debugf("Persisted new fork: %s/%s by %s",
                    persistent.id.repoOwner, persistent.id.repoName, persistent.id.forkOwnerLogin);
            return true;
        } else {
            Log.tracef("Fork already exists, skipping persistence: %s/%s by %s",
                    persistent.id.repoOwner, persistent.id.repoName, persistent.id.forkOwnerLogin);
            return false;
        }
    }

    /**
     * Counts the total number of locally registered forks for a given repository.
     * @param path Pair containing the owner (left) and repository name (right).
     * @return The number of local forks for this repository.
     */
    @Transactional
    public long count(Pair<String> path) {
        return count("id.repoOwner = ?1 and id.repoName = ?2",
                path.getLeft(),
                path.getRight()
        );
    }

    /**
     * Groups forks by month and year using a native SQL query.
     * @param technology The technology associated with these indicators.
     * @param pair Pair containing the repository owner and name.
     * @return A list of Indicator objects representing the number of forks per month.
     */
    @Transactional
    public List<Indicator> groupForksByMonths(Technology technology, Pair<String> pair) {
        Query extractionQuery = entityManager.createNativeQuery(groupForksByMonthsSql);
        extractionQuery.setParameter("repoOwner", pair.getLeft());
        extractionQuery.setParameter("repoName", pair.getRight());
        List<Object[]> results = extractionQuery.getResultList();
        return results.stream()
                .map(row -> toForkIndicator(technology, row))
                .collect(Collectors.toList());
    }

    /**
     * Converts a native SQL query result row into an Indicator object.
     * Expects the row to contain [Year, Month, Count].
     * @param technology The associated technology.
     * @param row An array of objects representing a result row.
     * @return An Indicator object.
     */
    private Indicator toForkIndicator(Technology technology, Object[] row) {
        LocalDate localDate = LocalDate.of(Integer.parseInt(row[0].toString()),
                Integer.parseInt(row[1].toString()), 1);
        Date d = Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
        return new Indicator(
                technology,
                GitHubForks.GITHUB_FORKS,
                d,
                row[2].toString()
        );
    }
}
