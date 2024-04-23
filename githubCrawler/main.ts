/**
 * @author Sylvain Gougouzian
 * @version 1.0
 */

import { saveJson } from "./utils";
import fetchSBOM from "./crawler/fetch";
import PackagistOrgFetch from "./packagist_org/fetch";
import NpmJsFetch from "./npmjs_com/fetch";
import CratesIoFetch from "./crates_io/fetch";
import prettyMilliseconds from "pretty-ms";
import sites from "./sites.json";

const useCache = Bun.env.USE_CACHE === "true";

const start = performance.now();

const sbomLibsPerLanguages = await fetchSBOM(sites, useCache);

saveJson("outputs/artifact.json", {
  // ...(await PackagistOrgFetch(sbomLibsPerLanguages.php, useCache)),
  // ...(await NpmJsFetch(sbomLibsPerLanguages.npm, useCache)),
  ...(await CratesIoFetch(sbomLibsPerLanguages.rust, false)),
});

console.log(
  prettyMilliseconds(performance.now() - start, {
    verbose: true,
  })
);
