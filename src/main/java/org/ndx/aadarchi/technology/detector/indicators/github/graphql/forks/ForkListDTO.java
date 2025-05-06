package org.ndx.aadarchi.technology.detector.indicators.github.graphql.forks;

import java.time.OffsetDateTime;
import java.util.List;

/*
* DTO for the fork history query response
* */
public class ForkListDTO {
    public Forks forks;

    public static class Forks {
        public int totalCount;
        public List<ForkNode> nodes;
        public PageInfo pageInfo;
    }

    public static class ForkNode {
        public OffsetDateTime createdAt;
        public ForkOwner owner;
    }

    public static class ForkOwner {
        public String login;
    }

    public static class PageInfo {
        public boolean hasPreviousPage;
        public String startCursor;
    }
}