package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.time.LocalDateTime;

record ArchivePoint(String urlkey, 
		LocalDateTime timestamp, 
		String original, 
		String digest,
		String length) {
}