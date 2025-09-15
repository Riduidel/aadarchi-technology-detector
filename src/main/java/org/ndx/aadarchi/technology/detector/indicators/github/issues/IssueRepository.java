package org.ndx.aadarchi.technology.detector.indicators.github.issues;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.apache.camel.util.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ndx.aadarchi.technology.detector.model.Indicator;
import org.ndx.aadarchi.technology.detector.model.Technology;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class IssueRepository implements PanacheRepository<Issue>{

    @ConfigProperty(name = "tech-trends.indicators.github.issues.sql.indicator")
    String groupIssuesByMonthsSql;
    private final EntityManager entityManager;

    public IssueRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Persists a issue event if it doesn't already exist.
     * @param persistent The Issue entity to persist.
     * @return true if the entity was persisted (didn't exist), false otherwise.
     */
    @Transactional
    public boolean maybePersist(Issue persistent) {
        if (count("id.owner = ?1 and id.repo = ?2 and id.date = ?3 and id.user = ?4",
        		persistent.id.owner,
                persistent.id.repo,
                persistent.id.date,
                persistent.id.user
        ) == 0) {
            persistent.persist();
            Log.debugf("Persisted new issue: %s/%s by %s",
                    persistent.id.owner, persistent.id.repo, persistent.id.user);
            return true;
        } else {
            Log.tracef("Issue already exists, skipping persistence: %s/%s by %s",
                    persistent.id.owner, persistent.id.repo, persistent.id.user);
            return false;
        }
    }

    /**
     * Counts the total number of locally registered issues for a given repository.
     * @param path Pair containing the owner (left) and repository name (right).
     * @return The number of local issues for this repository.
     */
    @Transactional
    public long count(Pair<String> path) {
        return count("id.owner = ?1 and id.repo = ?2",
                path.getLeft(),
                path.getRight()
        );
    }

    /**
     * Groups issues by month and year using a native SQL query.
     * @param technology The technology associated with these indicators.
     * @param pair Pair containing the repository owner and name.
     * @return A list of Indicator objects representing the number of issues per month.
     */
    @Transactional
    public List<Indicator> groupIssuesByMonths(Technology technology, Pair<String> pair) {
        Query extractionQuery = entityManager.createNativeQuery(groupIssuesByMonthsSql);
        extractionQuery.setParameter("owner", pair.getLeft());
        extractionQuery.setParameter("name", pair.getRight());
        List<Object[]> results = extractionQuery.getResultList();
        return results.stream()
                .map(row -> toIssueIndicator(technology, row))
                .collect(Collectors.toList());
    }

    /**
     * Converts a native SQL query result row into an Indicator object.
     * Expects the row to contain [Year, Month, Count].
     * @param technology The associated technology.
     * @param row An array of objects representing a result row.
     * @return An Indicator object.
     */
    private Indicator toIssueIndicator(Technology technology, Object[] row) {
        LocalDate localDate = LocalDate.of(Integer.parseInt(row[0].toString()),
                Integer.parseInt(row[1].toString()), 1);
        Date d = Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
        return new Indicator(
                technology,
                GitHubIssuesIndicatorComputer.GITHUB_ISSUES,
                d,
                row[2].toString()
        );
    }
}
