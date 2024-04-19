/**
 * @author Sylvain Gougouzian
 * @version 1.0
 */

import { top1000 as topPHPPackages } from "./packagistorg/top1000";
import { top1000 as topJSPackages } from "./npmjscom/top1000";
import { loadJson, saveJson } from "./utils";
import getGithubRepositories from "./crawler/getGithubRepositories";
import sites from "./sites.json";
import { getSBOM } from "./crawler/getSBOM";
import {
  cleanLibraries,
  getLibrairiesFromSBOM,
  sortLibrairiesByLanguages,
} from "./sbom.utils";
import { getBulkPackagesInfo as JSgetBulkPackagesInfo } from "./npmjscom/getBulkPackagesInfo";
import { getBulkPackagesInfo as PHPgetBulkPackagesInfo } from "./packagistorg/getBulkPackagesInfo";

console.log("Start analysis");

const useCache = true;

console.log("🔍 Top 1000 PHP packages 🐘");
let phpPackages;
if (useCache) {
  phpPackages = await loadJson("tmp/phpPackages.json");
} else {
  phpPackages = await topPHPPackages();
  saveJson("tmp/phpPackages.json", phpPackages);
}

console.log("🔍 Top 1000 JS packages 🟡");
let jsPackages;
if (useCache) {
  jsPackages = await loadJson("tmp/jsPackages.json");
} else {
  jsPackages = await topJSPackages();
  saveJson("tmp/jsPackages.json", jsPackages);
}

console.log("🔍 Analyse some sites to fetch most used repositories");
let githubRepositories;
if (useCache) {
  githubRepositories = await loadJson("tmp/githubRepositories.json");
} else {
  githubRepositories = await getGithubRepositories(sites);
  saveJson("tmp/githubRepositories.json", githubRepositories);
}
console.log(`🏴‍☠️ found ${githubRepositories.length} repositories`);

let sbomLibs;
if (useCache) {
  sbomLibs = await loadJson("tmp/sbom-libs.json");
} else {
  const promises: Promise<string[]>[] = [];
  githubRepositories.forEach((repo: string) => {
    promises.push(
      new Promise(async (resolve) => {
        // console.log(`📂 Analyse ${repo}`);
        const sbom = await getSBOM(repo);
        if (sbom) resolve(getLibrairiesFromSBOM(sbom));
        else resolve([]);
      })
    );
  });

  sbomLibs = cleanLibraries(
    Array.from(
      new Set(
        (await Promise.all(promises)).reduce((acc, val) => {
          return [...acc, ...val];
        }, [])
      )
    )
  ).sort();
  saveJson("tmp/sbom-libs.json", sbomLibs);
}

console.log(`🔍 Sorting ${sbomLibs.length} libraries`);

const sbomLibsPerLanguages = sortLibrairiesByLanguages(sbomLibs);
saveJson("outputs/artifact.json", {
  ...phpPackages,
  ...(await PHPgetBulkPackagesInfo(sbomLibsPerLanguages.php)),
  ...jsPackages,
  ...(await JSgetBulkPackagesInfo(sbomLibsPerLanguages.npm)),
});
