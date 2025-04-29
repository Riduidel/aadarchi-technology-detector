package org.ndx.aadarchi.technology.detector.indicators.github.graphql.forks;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import java.io.Serializable;
import java.util.Date;

@Entity(name = "GITHUB_FORKS")
public class Fork extends PanacheEntityBase {

    @Embeddable
    public static class ForkId implements Serializable {
        @Column(name = "REPO_OWNER") public String repoOwner;
        @Column(name = "REPO_NAME") public String repoName;
        @Column(name = "FORK_DATE") public Date forkedAt;
        @Column(name = "FORK_USER") public String forkOwnerLogin;

    }

    @EmbeddedId
    public Fork.ForkId id;

    public Fork() {
        super();
    }

    public Fork(String owner, String repo, Date date, String user) {
        this.id = new Fork.ForkId();
        this.id.repoOwner = owner;
        this.id.repoName = repo;
        this.id.forkedAt = date;
        this.id.forkOwnerLogin = user;
    }
}
