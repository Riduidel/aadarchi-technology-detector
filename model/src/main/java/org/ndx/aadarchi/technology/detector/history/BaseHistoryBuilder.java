package org.ndx.aadarchi.technology.detector.history;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
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
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;
import org.ndx.aadarchi.technology.detector.model.Formats;

import com.github.fge.lambdas.Throwing;

/**
 * Base class for history builder
 */
public abstract class BaseHistoryBuilder<Context extends ExtractionContext> {
	static final Logger logger = Logger.getLogger(BaseHistoryBuilder.class.getName());
	public final Path gitHistory;
	public final Path cache;
	public final String username;
	public final String email;
	public final File artifactsFile;
	public final String artifactsQualifier;
	public final String gitBranch;
	public final File schemaFile;
	
	static {
        String path = BaseHistoryBuilder.class.getClassLoader()
                .getResource("jul-log.properties")
                .getFile();
        System.setProperty("java.util.logging.config.file", path);
	}

	public BaseHistoryBuilder(Path cache, String gitUsername, String gitEmail, String artifactQualifierName) {
		super();
		this.cache = cache;
		gitHistory = cache.resolve("git-history");
		logger.warning(String.format("Using %s as git history repo", gitHistory));
		this.username = gitUsername;
		this.email = gitEmail;
		this.artifactsQualifier = artifactQualifierName;
		this.artifactsFile = new File(gitHistory.toFile(), artifactQualifierName + "/artifacts.json");
		this.schemaFile = new File(gitHistory.toFile(), artifactQualifierName + "/schema.json");
		gitBranch = "reports_" + artifactsQualifier;
	}

	protected abstract SortedMap<LocalDate, File> generateAggregatedStatusesMap(Context context,
			Collection<ArtifactDetails> allArtifactInformations) throws IOException;

	protected void commitArtifacts(Git git, LocalDate date, File artifacts, String commitMessage)
			throws GitAPIException, NoFilepatternException, AbortedByHookException, ConcurrentRefUpdateException,
			NoHeadException, NoMessageException, ServiceUnavailableException, UnmergedPathsException,
			WrongRepositoryStateException {
		ZoneId systemZoneId = ZoneId.systemDefault();
		Instant commitInstant = date.atStartOfDay(systemZoneId).toInstant();
		PersonIdent commiter = new PersonIdent(username, email, commitInstant, systemZoneId);
		String commitedFile = gitHistory.relativize(artifacts.toPath()).toString();
		git.add().addFilepattern(commitedFile).call();
		git.commit().setAuthor(commiter).setCommitter(commiter).setOnly(commitedFile).setAllowEmpty(false)
				.setMessage(commitMessage).call();
	}

	protected void writeArtifactListAtDate(Git git, LocalDate date, File inputFile, File commitedFilePath)
			throws IOException, AbortedByHookException, ConcurrentRefUpdateException, NoHeadException,
			NoMessageException, ServiceUnavailableException, UnmergedPathsException, WrongRepositoryStateException,
			GitAPIException {
		logger.info("Creating commit at " + date);
		Collection<ArtifactDetails> value = FileHelper.readFromFile(inputFile);
		FileUtils.copyFile(inputFile, commitedFilePath);
		// Then create a commit in the history repository
		commitArtifacts(git, date, commitedFilePath,
				String.format("Historical artifacts of %s, %d artifacts known at this date",
						Formats.MVN_DATE_FORMAT_WITH_DAY.format(date), value.size()));
	}

	protected void writeAggregatedStatusesToGit(Map<LocalDate, File> aggregatedStatuses, Git git) throws IOException {
		aggregatedStatuses.entrySet().stream().forEach(Throwing
				.consumer(entry1 -> writeArtifactListAtDate(git, entry1.getKey(), entry1.getValue(), artifactsFile)));
	}

	/**
	 * Initialize a valid git repository in a branch having a good default name
	 * 
	 * @return a git repository ready to accept commits
	 * @throws IOException
	 * @throws GitAPIException
	 * @throws TransportException
	 * @throws InvalidRemoteException
	 */
	Git initializeGitHistory() {
		File gitHstoryFile = gitHistory.toFile();
		File gitFile = new File(gitHstoryFile, ".git");
		try {
			if (gitFile.exists() && gitFile.isDirectory()) {
				logger.info(String.format("Reusing git history repository at %s", gitHstoryFile.getAbsolutePath()));
				return Git.open(gitHstoryFile);
			} else {
				logger.info(String.format("Creating git history repository at %s", gitHstoryFile.getAbsolutePath()));
				return createGitRepositoryHostingBranch(gitHstoryFile, gitBranch);
			}
		} catch (Exception e) {
			throw new RuntimeException("Can't clone or open git repo in " + gitHstoryFile.getAbsolutePath(), e);
		}
	}

	private Git createGitRepositoryHostingBranch(File gitHstoryFile, String branch)
			throws GitAPIException, URISyntaxException {
		Git git = Git.init().setDirectory(gitHstoryFile).setInitialBranch(branch).call();
		git.remoteAdd().setName("origin").setUri(new URIish("https://github.com/Riduidel/aadarchi-technology-detector.git"))
				.call();
		// Don't forget to fetch first
		git.fetch().call();
		List<Ref> remoteBranches = git.branchList().setListMode(ListMode.ALL).call();
		boolean branchExistsRemotely = remoteBranches.stream()
				.filter(ref -> ref.getName().endsWith("/"+branch))
				.findAny()
				.isPresent();
		// If branch exists on remote, clone it
		if (branchExistsRemotely) {
			git.pull().setCredentialsProvider(null).call();
		} else {
			logger.warning("You specified the branch " + branch + " which doesn't exists yet on remote");
		}
		return git;
	}

	/**
	 * Subverts all this class mechanism to generate a complete git history for all
	 * monthly statistics of all the given artifacts.
	 * 
	 * @param context
	 * @param allArtifactInformations
	 * @throws IOException
	 * @throws GitAPIException 
	 * @throws NoHeadException 
	 */
	public void generateHistoryFor(Context context, Collection<ArtifactDetails> allArtifactInformations)
			throws IOException, NoHeadException, GitAPIException {
		Git git = initializeGitHistory();
		// To get commits of branch, we have to first list branches
		// Hopefully that repo should contain only our branch
		List<Ref> branches = git.branchList().call();
		Ref branchName = branches.stream().filter(ref -> ref.getName().contains(gitBranch)).findAny().get();
		Iterable<RevCommit> commits = git.log()
			.add(branchName.getObjectId())
			.call();
		// We consider history to be of the good size, so if the git repository already exists,
		// it means it should be "augmented" with whatever augmenters are available
		if(commits.iterator().hasNext()) {
			// Time for an history augmentation!
			new HistoryAugmenter<Context>(this).augmentHistory(context, git);
		} else {
			// This branch is new, so get all data and write it!
			Map<LocalDate, File> aggregatedStatuses = generateAggregatedStatusesMap(context, allArtifactInformations);
			// Write them into git history
			writeAggregatedStatusesToGit(aggregatedStatuses, git);
		}
	}
}
