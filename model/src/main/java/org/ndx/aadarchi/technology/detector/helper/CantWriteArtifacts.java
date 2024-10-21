package org.ndx.aadarchi.technology.detector.helper;

import java.io.IOException;

import org.ndx.aadarchi.technology.detector.exceptions.ExtractionException;

public class CantWriteArtifacts extends ExtractionException {

	public CantWriteArtifacts(String string, Exception e) {
		super(string, e);
	}

}
