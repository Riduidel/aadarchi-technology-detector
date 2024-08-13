package org.ndx.aadarchi.technology.detector.npmjs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.ndx.aadarchi.technology.detector.helper.BaseHistoryBuilder;
import org.ndx.aadarchi.technology.detector.helper.FileHelper;
import org.ndx.aadarchi.technology.detector.helper.NoContext;
import org.ndx.aadarchi.technology.detector.model.ArtifactDetails;

import com.github.fge.lambdas.Throwing;

class HistoryBuilder extends BaseHistoryBuilder<NoContext> {
	static final Logger logger = Logger.getLogger(HistoryBuilder.class.getName());

		HistoryBuilder(Path gitHistory, Path cache) {
			super(gitHistory, cache, 
			    	"🤖 Npmjs History Builder",
					"get_npmjs_infos.yaml@history", 
			    	new File(new File(gitHistory.toFile(), "npmjs"), "artifacts.json"));
		}

		public static DateTimeFormatter DATE_FORMAT_WITH_DAY =
				new DateTimeFormatterBuilder()
					.appendPattern("MMM dd, yyyy")
					.parseCaseInsensitive()
					.toFormatter(Locale.ENGLISH)
					;

		private File getDatedFilePath(File containerDir,
				LocalDate timestamp) {
			return FileUtils.getFile(containerDir, 
					timestamp.getYear()+"", 
					timestamp.getMonthValue()+"",
					timestamp.getDayOfMonth()+"",
					"artifacts.json");
		}

		private File  generateHistoryAtMonth(Collection<ArtifactDetails> allDetails,
				LocalDate month) throws IOException {
			File destination = getDatedFilePath(new File(cache.toFile(), "captures"), month);
			if(!destination.exists()) {
				DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				String monthlySearch = String.format("%s:%s",
						format.format(month),
						format.format(month.withDayOfMonth(month.getMonth().length(month.isLeapYear())))
						);
				Collection<ArtifactDetails> atMonth = ExtractPopularNpmjsArtifacts.getAllDownloadsForPeriod(allDetails, monthlySearch);
		        FileHelper.writeToFile(atMonth, destination);

			}
			return destination;
		}

		@Override
		protected SortedMap<LocalDate, File> generateAggregatedStatusesMap(NoContext context,
				Collection<ArtifactDetails> allDetails) throws IOException {
	    	LocalDate initial = LocalDate.of(2010, 1, 1);
	    	logger.info("Fetching all dependencies since "+initial);
	    	SortedMap<LocalDate, File> aggregatedStatuses = initial.datesUntil(LocalDate.now(), Period.ofMonths(1))
	    		.map(Throwing.function(month -> Map.entry(month, generateHistoryAtMonth(allDetails, month))))
	    		.filter(Throwing.predicate(entry -> {
	    			Collection<ArtifactDetails> infos = FileHelper.readFromFile(entry.getValue());
	    			return !infos.isEmpty();
	    		}))
//	    		.forEach(entry -> logger.info("Got downloads at "+entry.getKey()))
	    		.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, () -> new TreeMap<LocalDate, File>()))
	    		;
	    	logger.info("Creating commits for all dates since "+initial);
	    	return aggregatedStatuses;
		}
		
	}