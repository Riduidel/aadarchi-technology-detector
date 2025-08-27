package org.ndx.aadarchi.technology.detector.model;

import org.ndx.aadarchi.technology.detector.librariesio.model.Project;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TechnologyRepository implements PanacheRepository<Technology> {

	@Transactional
	public Technology findOrCreateFromLibrariesIOLibrary(Project body) {
		Technology returned = null;
		// First find the reference url
		if(body.getPackageManagerUrl() != null) {
			returned = find("packageManagerUrl", body.getPackageManagerUrl()).firstResult();
			if(returned!=null) {
				return returned;
			}
		}
		// If not found, create it and persist it immediatly
		returned = newTechnology(body);
		persist(returned);
		return returned;
	}

	private Technology newTechnology(Project body) {
		Technology returned = new Technology();
		returned.name = body.getName();
		returned.description = body.getDescription();
		returned.homepage = body.getHomepage();
		returned.packageManagerUrl = body.getPackageManagerUrl();
		returned.repositoryUrl = body.getRepositoryUrl();
		returned.platform = body.getPlatform();
		return returned;
		
	}

}
