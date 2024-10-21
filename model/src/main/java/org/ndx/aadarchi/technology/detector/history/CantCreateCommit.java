package org.ndx.aadarchi.technology.detector.history;

import java.io.IOException;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CantCreateCommit extends ExtractionException {

	public CantCreateCommit(String string, Exception e) {
		super(string, e);
	}

}
