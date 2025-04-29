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
     * Persiste un événement de fork s'il n'existe pas déjà.
     * @param persistent L'entité Fork à persister.
     * @return true si l'entité a été persistée (n'existait pas), false sinon.
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
     * Compte le nombre total de forks enregistrés localement pour un dépôt donné.
     * @param path Paire contenant le propriétaire (gauche) et le nom du dépôt (droite).
     * @return Le nombre de forks locaux pour ce dépôt.
     */
    @Transactional
    public long count(Pair<String> path) {
        return count("id.repoOwner = ?1 and id.repoName = ?2",
                path.getLeft(),
                path.getRight()
        );
    }

    /**
     * Regroupe les forks par mois et année en utilisant une requête SQL native.
     * @param technology La technologie associée à ces indicateurs.
     * @param pair Paire contenant le propriétaire et le nom du dépôt.
     * @return Une liste d'objets Indicator représentant le nombre de forks par mois.
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
     * Convertit une ligne de résultat de la requête SQL native en un objet Indicator.
     * S'attend à ce que la ligne contienne [Année, Mois, Compte].
     * @param technology La technologie associée.
     * @param row Un tableau d'objets représentant une ligne de résultat.
     * @return Un objet Indicator.
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
