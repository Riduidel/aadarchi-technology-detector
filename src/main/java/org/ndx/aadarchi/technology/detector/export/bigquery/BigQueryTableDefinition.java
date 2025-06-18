package org.ndx.aadarchi.technology.detector.export.bigquery;

import java.util.Comparator;

public record BigQueryTableDefinition (String tablePath, 
		String readQuery, 
		String findLatest, 
		String logMessage,
		boolean readAsStream
		) {
}