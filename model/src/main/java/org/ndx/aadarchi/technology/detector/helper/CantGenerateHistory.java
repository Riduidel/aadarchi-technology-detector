package org.ndx.aadarchi.technology.detector.helper;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CantGenerateHistory extends ExtractionException {

	public CantGenerateHistory(String string, Exception e) {
		super(string, e);
	}

}
