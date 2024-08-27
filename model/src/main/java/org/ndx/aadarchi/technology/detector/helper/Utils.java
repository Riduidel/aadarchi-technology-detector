package org.ndx.aadarchi.technology.detector.helper;

import java.net.MalformedURLException;
import java.net.URL;

public class Utils {
	public static String getDomain(String url) {
		try {
			return new URL(url).getHost();
		} catch(MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
