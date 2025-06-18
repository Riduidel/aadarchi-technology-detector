package org.ndx.aadarchi.technology.detector.indicators.github.forks;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

import org.ndx.aadarchi.technology.detector.export.bigquery.annotations.BigQueryTable;

@BigQueryTable
@Entity
@Table(name = "GITHUB_FORKS")
public class Fork extends PanacheEntityBase {

    @Embeddable
    public static class ForkId implements Serializable {
        @Column(name = "REPO_OWNER") public String owner;
        @Column(name = "REPO_NAME") public String repo;
        @Column(name = "FORK_DATE") public Date date;
        @Column(name = "FORK_USER") public String user;

    }

    @EmbeddedId
    public Fork.ForkId id;

    public Fork() {
        super();
    }

    public Fork(String owner, String repo, Date date, String user) {
        this.id = new Fork.ForkId();
        this.id.owner = owner;
        this.id.repo = repo;
        this.id.date = date;
        this.id.user = user;
    }
}
