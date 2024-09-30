package org.ndx.aadarchi.technology.detector.history;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.lib.Ref;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ndx.aadarchi.technology.detector.loader.NoContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

class BaseHistoryBuilderTest {
	public static class Tested extends BaseHistoryBuilder<NoContext> {

		public Tested(Path cache, String extension) {
			super(cache, "🤖 Test History Builder", 
					"get_test_infos.yaml@history",
					extension);
		}

		@Override
		protected SortedMap<LocalDate, File> generateAggregatedStatusesMap(NoContext context,
				Collection<ArtifactDetails> allArtifactInformations) throws IOException {
			return new TreeMap<LocalDate, File>();
		}
		
	}

	@Test
	void can_create_git_repository_when_missing(@TempDir Path cache) throws GitAPIException {
		// Given
		Tested t = new Tested(cache, "can_create_git_repository_when_missing");
		// When
		Git git = t.initializeGitHistory();
		// Then
		Assertions.assertThat(git)
			.isNotNull();
		Assertions.assertThat(git.getRepository().getDirectory())
			.exists()
			.isNotEmptyDirectory();
		List<Ref> branches = git.branchList().call();
		// Since branch doesn't exists remotely, it is not cloned and never exists locally
		Assertions.assertThat(branches)
			.isEmpty()
			;
	}

	@Test
	void can_clone_git_repository_when_existing(@TempDir Path cache) throws GitAPIException {
		// Given
		Tested t = new Tested(cache, "mvnrepository");
		// When
		Git git = t.initializeGitHistory();
		// Then
		Assertions.assertThat(git)
			.isNotNull();
		Assertions.assertThat(git.getRepository().getDirectory())
			.exists()
			.isNotEmptyDirectory();
		// We should have one branch present (it exists remotely)
		List<Ref> branches = git.branchList().call();
		Assertions.assertThat(branches)
			.isNotEmpty()
			;
	}

	@Test
	void can_reuse_git_repository_when_present_locally(@TempDir Path cache) throws InvalidRefNameException, IllegalStateException, GitAPIException {
		// Given
		Tested t = new Tested(cache, "can_create_git_repository_when_missing");
		// We have to initialize repository here
		String BRANCH_NAME = "a_random_branch";
		Git preexisting = Git.init().setDirectory(t.gitHistory.toFile()).setInitialBranch(BRANCH_NAME).call();
		preexisting.commit()
			.setAllowEmpty(true)
			.setMessage("Without commit, the branch won't be created")
			.call();
		// When
		Git git = t.initializeGitHistory();
		// Then
		List<Ref> branches = git.branchList().call();
		Assertions.assertThat(branches)
			.isNotEmpty()
			.extracting(Ref::getName)
			.extracting(name -> name.substring(name.lastIndexOf('/')+1))
			.contains(BRANCH_NAME);
	}
}
