package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

public class GitHubProjects {
	public static final String GITHUB_REPOSITORIES = "github.repositories.properties";

	private static final Properties githubProjects;

	static {
		githubProjects = new Properties();
		try(InputStream input = GitHubProjects.class.getClassLoader().getResourceAsStream(GITHUB_REPOSITORIES)) {
			githubProjects.load(input);
		} catch (IOException e) {
			throw new RuntimeException("Can't read "+GITHUB_REPOSITORIES, e);
		}
	}

	public static Properties get() {
		return githubProjects;
	}

	public static String getGitHubPath(ArtifactDetails details) {
		if(get().containsKey(details.getCoordinates())) {
			return getGitHubPath(get().getProperty(details.getCoordinates()));
		} else if(get().containsKey(details.getGroupId())) {
			return getGitHubPath(get().getProperty(details.getGroupId()));
		} else {
			return null;
		}
		
	}

	public static String getGitHubPath(String githubRepositoryUrl) {
		String GITHUB = "github.com/";
		String returned = githubRepositoryUrl.substring(githubRepositoryUrl.indexOf(GITHUB)+GITHUB.length());
		String[] parts = returned.split("/");
		if(parts.length<=2) {
			return returned;
		} else {
			return parts[0]+"/"+parts[1];
		}
	}

	public static boolean contains(ArtifactDetails source) {
		return get().containsKey(source.getCoordinates()) || get().containsKey(source.getGroupId());
	}

}
