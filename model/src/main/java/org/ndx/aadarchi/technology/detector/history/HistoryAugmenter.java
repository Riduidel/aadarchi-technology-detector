package org.ndx.aadarchi.technology.detector.history;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.ndx.aadarchi.technology.detector.augmenters.Augmenters;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.loader.ExtractionContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.github.fge.lambdas.Throwing;

public class HistoryAugmenter<Context extends ExtractionContext> {
	static final Logger logger = Logger.getLogger(HistoryAugmenter.class.getName());
	public final String gitBranchName;
	public final String replacerBranchName;
	public final File artifactsFile;
	public final File schemaFile;
	public final Path gitHistory;

	public HistoryAugmenter(BaseHistoryBuilder<Context> baseHistoryBuilder) {
		this.gitHistory = baseHistoryBuilder.gitHistory;
		this.gitBranchName = baseHistoryBuilder.gitBranch;
		this.artifactsFile = baseHistoryBuilder.artifactsFile;
		this.schemaFile = baseHistoryBuilder.schemaFile;
		replacerBranchName = gitBranchName + "_REPLACER";
	}

	/**
	 * Augment history by navigating all commits and, for each commit
	 * <ul>
	 * <li>Checkout commit
	 * <li>Load artifacts
	 * <li>Apply known augmenters to commit
	 * <li>Commit file to "another branch"</li> And finally replace inital branch by
	 * updated one
	 * 
	 * @param git
	 */
	public void augmentHistory(Context context, Git git) throws IOException, GitAPIException {
		// First, create augmented branch from scratch
		Ref checkout = git.checkout().setName(gitBranchName).call();
		// This branch will have an upcased suffix to make sure I can't ignore it
		git.branchList().call().stream().filter(ref -> ref.getName().endsWith(replacerBranchName)).findAny()
			.ifPresent(
				Throwing.consumer(
					branch -> {
						logger.info(String.format("Output branch %s is not clean, removing", replacerBranchName));
						git.branchDelete()
							.setBranchNames(replacerBranchName)
							.setForce(true)
							.call();
					}));
		List<Ref> branches = git.branchList().call();
		Ref gitBranch = branches.stream().filter(ref -> ref.getName().contains(gitBranchName)).findAny().get();
		// Now we have a target branch, take each commit in turn
		Iterable<RevCommit> commits = git.log()
				.add(gitBranch.getObjectId())
				.call();
		StreamSupport.stream(commits.spliterator(), false)
			.sorted(Comparator.comparing(c -> c.getCommitterIdent().getWhen()))
			.forEach(Throwing.consumer(revcommit -> augmentCommit(context, git, revcommit)));
	}

	/**
	 * This method takes some commit from the source branch,
	 * load data
	 * apply augmenters
	 * write augmented data to replacer branch
	 * @param context 
	 * @throws GitAPIException 
	 * @throws CheckoutConflictException 
	 * @throws InvalidRefNameException 
	 * @throws RefNotFoundException 
	 * @throws RefAlreadyExistsException 
	 */
	private void augmentCommit(Context context, Git git, RevCommit commit) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException, IOException {
		logger.info(String.format("Checking out %s (at date %s) with short message \"%s\"", 
				commit.getName(),
				commit.getCommitterIdent().getWhen(),
				commit.getShortMessage()));
		git.checkout().setName(commit.getName()).call();
		LocalDate commitDate = 
				LocalDate.ofInstant( 
						commit.getAuthorIdent().getWhen().toInstant(),
						ZoneOffset.UTC);
		// Don't forget to read schema!
		Optional<String> schema = schemaFile.exists()
				? Optional.of(FileUtils.readFileToString(schemaFile, "UTF-8"))
				: Optional.empty()
				;
		// Now load artifacts into data structure
		List<ArtifactDetails> artifacts = FileHelper.readFromFile(artifactsFile, ArtifactDetails.LIST);
		Collection<ArtifactDetails> augmented = Augmenters.augmentArtifacts(context, artifacts, commitDate);
		// Switch branch
		// If branch doesn't exist yet, create an orphan one
		// (for more details, see https://stackoverflow.com/a/59162735/15619)
		List<Ref> branches = git.branchList().call();
		boolean replacerAlreadyExists = branches.stream()
				.anyMatch(ref -> ref.getName().endsWith(replacerBranchName));
		git.checkout()
			.setOrphan(!replacerAlreadyExists)
//			.setCreateBranch(!replacerAlreadyExists)
			.setName(replacerBranchName)
			.call();
		// Write the file
		FileHelper.writeToFile(augmented, artifactsFile);
		// And the schema
		schema.ifPresent(Throwing.consumer(s -> 
			FileUtils.writeStringToFile(schemaFile, s, Charset.forName("UTF-8"))));
		// Commit with all values read from initial commit
		PersonIdent commiter = commit.getAuthorIdent();
		String commitMessage = commit.getFullMessage();
		Status response = git.status().call();
		if(!response.getAdded().isEmpty()) {
			AddCommand addCommand = git.add();
			for(String s : response.getAdded()) {
				addCommand.addFilepattern(s);
			}
			addCommand.call();
		}
		git.commit()
			.setAuthor(commiter)
			.setCommitter(commiter)
//			.setOnly(commitedFile)
			.setAll(true)
			.setAllowEmpty(true)
			.setMessage(commitMessage)
			.call();
	}
}
