/**
 * @author Sylvain Gougouzian
 * @version 1.0
 */

import { top1000 as topPHPPackages } from "./packagistorg/top1000";
import { top1000 as topJSPackages } from "./npmjscom/top1000";
import { saveJson } from "./utils";
import getGithubRepositories from "./crawler/getGithubRepositories";
import sites from "./sites.json";
import { getSBOM } from "./crawler/getSBOM";
import { getLibrairiesFromSBOM, sortLibrairiesByLanguages } from "./sbom.utils";
import { getBulkPackagesInfo as JSgetBulkPackagesInfo } from "./npmjscom/getBulkPackagesInfo";
import { getBulkPackagesInfo as PHPgetBulkPackagesInfo } from "./packagistorg/getBulkPackagesInfo";

console.log("Start analysis");
console.log("🔍 Top 1000 PHP packages 🐘");
// const phpPackages = await topPHPPackages();

console.log("🔍 Top 1000 JS packages 🟡");
// const jsPackages = await topJSPackages();

console.log("🔍 Analyse some sites to fetch most used repositories");
// const githubRepositories = await getGithubRepositories(sites);
// console.log(githubRepositories);

const sbom = await getSBOM("https://github.com/PrestaShop/PrestaShop");
const sbomLibs = sortLibrairiesByLanguages(getLibrairiesFromSBOM(sbom));

saveJson("outputs/artifact.json", {
  ...JSgetBulkPackagesInfo(sbomLibs.npm),
  ...PHPgetBulkPackagesInfo(sbomLibs.php),
});
