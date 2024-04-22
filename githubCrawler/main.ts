/**
 * @author Sylvain Gougouzian
 * @version 1.0
 */

import { saveJson } from "./utils";
import fetchSBOM from "./crawler/fetch";
import PackagistOrgFetch from "./packagistorg/fetch";
import NpmJsFetch from "./npmjscom/fetch";
import CratesIoFetch from "./cratesio/fetch";

const useCache = true;

const sbomLibsPerLanguages = await fetchSBOM(useCache);

saveJson("outputs/artifact.json", {
  // ...(await PackagistOrgFetch(sbomLibsPerLanguages.php, useCache)),
  // ...(await NpmJsFetch(sbomLibsPerLanguages.npm, useCache)),
  ...(await CratesIoFetch(sbomLibsPerLanguages.rust, useCache)),
});
