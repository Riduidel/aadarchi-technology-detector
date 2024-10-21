package org.ndx.aadarchi.technology.detector.exceptions;

import java.io.IOException;

public class CannottReadFromCache extends ExtractionException {

	public CannottReadFromCache(String string, Exception e) {
		super(string, e);
	}

}
