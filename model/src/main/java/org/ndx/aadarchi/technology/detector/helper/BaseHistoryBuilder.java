package org.ndx.aadarchi.technology.detector.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.ServiceUnavailableException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.URIish;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.Formats;

import com.github.fge.lambdas.Throwing;

/**
 * Base class for history builder
 */
public abstract class BaseHistoryBuilder<Context extends ExtractionContext> {
	static final Logger logger = Logger.getLogger(BaseHistoryBuilder.class.getName());
	protected final Path gitHistory;
	protected final Path cache;
	protected final String username;
	protected final String email;
	protected final File artifactsFile;
	private final String artifactsQualifier;
	
	public BaseHistoryBuilder(Path cache, String gitUsername, String gitEmail, String artifactQualifierName) {
		super();
		this.cache = cache;
		gitHistory = cache.resolve("git-history");
		logger.warning(String.format("Using %s as git history repo", gitHistory));
		this.username = gitUsername;
		this.email = gitEmail;
		this.artifactsQualifier = artifactQualifierName;
		this.artifactsFile= new File(gitHistory.toFile(), artifactQualifierName+"/artifacts.json");
	}
	
	protected abstract SortedMap<LocalDate, File> generateAggregatedStatusesMap(Context context,
			Collection<ArtifactDetails> allArtifactInformations) throws IOException;

		protected void commitArtifacts(Git git, LocalDate date, File artifacts, String commitMessage)
			throws GitAPIException, NoFilepatternException, AbortedByHookException, ConcurrentRefUpdateException,
			NoHeadException, NoMessageException, ServiceUnavailableException, UnmergedPathsException,
			WrongRepositoryStateException {
		ZoneId systemZoneId = ZoneId.systemDefault();
		Instant commitInstant = date.atStartOfDay(systemZoneId).toInstant();
		PersonIdent commiter = new PersonIdent(username,
				email, commitInstant, systemZoneId);
		String commitedFile = gitHistory.relativize(artifacts.toPath()).toString();
		git.add()
			.addFilepattern(commitedFile)
			.call();
		git.commit()
			.setAuthor(commiter)
			.setCommitter(commiter)
			.setOnly(commitedFile)
			.setAllowEmpty(false)
			.setMessage(commitMessage)
			.call();
	}

	protected void writeArtifactListAtDate(Git git, LocalDate date, File inputFile, File commitedFilePath)
			throws IOException, AbortedByHookException, ConcurrentRefUpdateException, NoHeadException,
			NoMessageException, ServiceUnavailableException, UnmergedPathsException,
			WrongRepositoryStateException, GitAPIException {
		logger.info("Creating commit at " + date);
		Collection<ArtifactDetails> value = FileHelper.readFromFile(inputFile);
		FileUtils.copyFile(inputFile, commitedFilePath);
		// Then create a commit in the history repository
		commitArtifacts(git, date, commitedFilePath,
				String.format("Historical artifacts of %s, %d artifacts known at this date",
						Formats.MVN_DATE_FORMAT_WITH_DAY.format(date), value.size()));
	}

	protected void writeAggregatedStatusesToGit(Map<LocalDate, File> aggregatedStatuses) throws IOException {
		Git git = initializeGitHistory();
		aggregatedStatuses.entrySet().stream()
				.forEach(Throwing.consumer(entry1 -> writeArtifactListAtDate(git, entry1.getKey(), entry1.getValue(), artifactsFile)));
	}

	/**
	 * Initialize a valid git repository in a branch having a good default name
	 * @return a git repository ready to accept commits
	 * @throws IOException
	 * @throws GitAPIException 
	 * @throws TransportException 
	 * @throws InvalidRemoteException 
	 */
	private Git initializeGitHistory() {
		File gitHstoryFile = gitHistory.toFile();
		File gitFile = new File(gitHstoryFile, ".git");
		try {
			if(gitFile.exists() && gitFile.isDirectory()) {
				return Git.open(gitHstoryFile);
			} else {
				Git git = Git.init()
					.setDirectory(gitHstoryFile)
					.setInitialBranch("reports_pypi")
					.call();
				git.remoteAdd()
					.setName("origin")
					.setUri(new URIish("git@github.com:Riduidel/aadarchi-technology-detector.git"))
					.call();
				return git;
			}
		} catch(Exception e) {
			throw new RuntimeException("Can't clone or open git repo in "+gitHstoryFile.getAbsolutePath(), e);
		}
	}

	/**
	 * Subverts all this class mechanism to generate a complete git history for all
	 * monthly statistics of all the given artifacts.
	 * 
	 * @param context
	 * @param allArtifactInformations
	 * @throws IOException
	 */
	public void generateHistoryFor(Context context, Collection<ArtifactDetails> allArtifactInformations) throws IOException {
		Map<LocalDate, File> aggregatedStatuses = generateAggregatedStatusesMap(context, allArtifactInformations);
		// Write them into git history
		writeAggregatedStatusesToGit(aggregatedStatuses);
	}
}