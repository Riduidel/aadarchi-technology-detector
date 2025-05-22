package org.ndx.aadarchi.technology.detector.indicators.downloads.npm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.ndx.aadarchi.technology.detector.model.Technology;

import io.quarkus.logging.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@RegisterRestClient(configKey = "npmjs-api")
public interface NpmjsRestClient {
	/**
	 * @see https://github.com/npm/registry/blob/main/docs/download-counts.md
	 */
	@GET
	@Path("/downloads/point/{periodStart}:{periodEnd}/{artifactName}")
	public DownloadCount getDownloadCountInRange(@PathParam("periodStart") String periodStart,
			@PathParam("periodEnd") String periodEnd,
			@PathParam("artifactName") String artifactId);

	public default DownloadCount getDownloadCountBetween(LocalDate previousMonth, LocalDate month, Technology technology) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");
		String technologyName = technology.packageManagerUrl.substring("https://www.npmjs.com/package/".length());
		Log.infof("Getting download count during period [%s to %s] for %s", previousMonth, month, technologyName);
		return getDownloadCountInRange(
				formatter.format(previousMonth), 
				formatter.format(month),
				technologyName);
	}
}
