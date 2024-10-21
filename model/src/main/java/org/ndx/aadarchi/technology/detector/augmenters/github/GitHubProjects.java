package org.ndx.aadarchi.technology.detector.augmenters.github;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Function;

import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

public class GitHubProjects {
	public static final String GITHUB_REPOSITORIES = "github.repositories.properties";

	private static final Properties githubProjects;

	static {
		githubProjects = new Properties();
		if(GitHubProjects.class.getClassLoader().getResource(GITHUB_REPOSITORIES)!=null) {
			try(InputStream input = GitHubProjects.class.getClassLoader().getResourceAsStream(GITHUB_REPOSITORIES)) {
				githubProjects.load(input);
			} catch (IOException e) {
				throw new RuntimeException("Can't read "+GITHUB_REPOSITORIES, e);
			}
		}
	}

	public static Properties get() {
		return githubProjects;
	}

	public static String getGitHubPath(ArtifactDetails details) {
		for(Function<ArtifactDetails, String> extractor : ArtifactDetails.GITHUB_REPO_EXTRACTORS) {
			String key = extractor.apply(details);
			if(key!=null) {
				if(get().containsKey(key)) {
					return getGitHubPath(get().getProperty(key));
				}
			}
		}
		return null;
	}

	public static String getGitHubPath(String githubRepositoryUrl) {
		String GITHUB = "github.com/";
		String returned = githubRepositoryUrl.substring(githubRepositoryUrl.indexOf(GITHUB)+GITHUB.length());
		String[] parts = returned.split("[/#]");
		if(parts.length<=2) {
			return returned;
		} else {
			return parts[0]+"/"+parts[1];
		}
	}

	public static boolean contains(ArtifactDetails source) {
		for(Function<ArtifactDetails, String> extractor : ArtifactDetails.GITHUB_REPO_EXTRACTORS) {
			String key = extractor.apply(source);
			if(key!=null && get().containsKey(key))
				return true;
		}
		return false;
	}

}
