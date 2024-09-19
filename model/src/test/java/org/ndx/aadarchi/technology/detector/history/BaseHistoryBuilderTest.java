package org.ndx.aadarchi.technology.detector.history;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.Git;
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
	void can_create_git_repository_when_missing(@TempDir Path cache) {
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
	}

}
