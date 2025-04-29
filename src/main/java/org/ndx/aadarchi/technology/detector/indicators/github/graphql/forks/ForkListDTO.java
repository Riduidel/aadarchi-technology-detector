package org.ndx.aadarchi.technology.detector.indicators.github.graphql.models;

import java.time.OffsetDateTime;
import java.util.List;

/*
* DTO pour la réponse de la requête d'historique des forks
* */
public class ForkListDTO {
    public Forks forks;

    public static class Forks {
        public int totalCount;
        public List<ForkNode> nodes;
        public PageInfo pageInfo;
    }

    public static class ForkNode {
        // Assurez-vous que le type correspond à ce que l'API GraphQL renvoie
        // Si c'est une String ISO 8601, vous devrez la parser.
        // Si SmallRye GraphQL Client peut le faire automatiquement, utilisez OffsetDateTime.
        public OffsetDateTime createdAt;
        public ForkOwner owner;
    }

    public static class ForkOwner {
        public String login;
    }

    // Réutilisez la classe PageInfo existante si elle est définie ailleurs,
    // sinon définissez-la ici ou dans une classe commune.
    public static class PageInfo {
        public boolean hasPreviousPage;
        public String startCursor;
    }
}