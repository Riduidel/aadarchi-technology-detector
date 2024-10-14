package org.ndx.aadarchi.technology.detector.npmjs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.history.BaseHistoryBuilder;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.fge.lambdas.Throwing;

class HistoryBuilder extends BaseHistoryBuilder<NpmjsContext> {
	static final Logger logger = Logger.getLogger(HistoryBuilder.class.getName());

	HistoryBuilder(ExtractPopularNpmjsArtifacts extractPopularNpmjsArtifacts, Path gitHistory, Path cache, boolean forceRebuildHistory) {
		super(cache, "🤖 Npmjs History Builder", 
		    	"get_npmjs_infos.yaml@history",
				"npmjs", forceRebuildHistory);
	}

	private File getDatedFilePath(File containerDir,
			LocalDate timestamp) {
		return FileUtils.getFile(containerDir, 
				timestamp.getYear()+"", 
				timestamp.getMonthValue()+"",
				timestamp.getDayOfMonth()+"",
				"artifacts.json");
	}

	private File  generateHistoryAtMonth(NpmjsContext context, Collection<ArtifactDetails> allDetails,
			LocalDate month) throws IOException {
		File destination = getDatedFilePath(new File(cache.toFile(), "captures"), month);
		if(!destination.exists()) {
			DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			String monthlySearch = String.format("%s:%s",
					format.format(month),
					format.format(month.withDayOfMonth(month.getMonth().length(month.isLeapYear())))
					);
			Collection<ArtifactDetails> atMonth = context.getAllDownloadsForPeriod(allDetails, monthlySearch);
	        FileHelper.writeToFile(atMonth, destination);

		}
		return destination;
	}

	@Override
	protected SortedMap<LocalDate, File> generateAggregatedStatusesMap(NpmjsContext context,
			Collection<ArtifactDetails> allDetails) throws IOException {
    	LocalDate initial = LocalDate.of(2010, 1, 1);
    	logger.info("Fetching all dependencies since "+initial);
    	SortedMap<LocalDate, File> aggregatedStatuses = initial.datesUntil(LocalDate.now(), Period.ofMonths(1))
    		.map(Throwing.function(month -> Map.entry(month, generateHistoryAtMonth(context, allDetails, month))))
    		.filter(Throwing.predicate(entry -> {
    			Collection<ArtifactDetails> infos = FileHelper.readFromFile(entry.getValue(), ArtifactDetails.LIST);
    			return !infos.isEmpty();
    		}))
//	    		.forEach(entry -> logger.info("Got downloads at "+entry.getKey()))
    		.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, () -> new TreeMap<LocalDate, File>()))
    		;
    	logger.info("Creating commits for all dates since "+initial);
    	return aggregatedStatuses;
	}
}