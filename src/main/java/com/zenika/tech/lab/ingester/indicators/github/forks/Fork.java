package com.zenika.tech.lab.ingester.indicators.github.forks;

import java.io.Serializable;
import java.util.Date;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;

@Entity(name = "GITHUB_FORKS")
@NamedNativeQueries({
	 @NamedNativeQuery(
			 name="GITHUB_FORKS.CSV.EXPORT", 
			 query="""
select 
to_char(fork_date, 'YYYY-MM-DD HH:MM:SS') as fork_date,
repo_owner,
repo_name,
fork_user
from github_forks;
			 		""")
})
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
