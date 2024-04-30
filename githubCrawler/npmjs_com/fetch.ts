import { loadJson, saveJson } from "../utils";
import getBulkPackagesInfo from "./getBulkPackagesInfo";
import { top1000 } from "./top1000";

const NpmJsFetch = async (
  sbomPackages: string[],
  useCache: boolean = false
) => {
  console.log("🔍 Analyse npmjs.com 🟡");
  console.log("  🏅 Top 1000");
  let jsPackages;
  if (useCache) {
    jsPackages = await loadJson("tmp/jsPackages.json");
  } else {
    jsPackages = await top1000();
    saveJson("tmp/jsPackages.json", jsPackages);
  }
  console.log(`  📋 SBOM ${sbomPackages.length} packages`);
  return {
    ...jsPackages,
    ...(await getBulkPackagesInfo(sbomPackages, {
      withDownloads: true,
      withScopedDownloads: false, // too much queries
    })),
  };
};

export default NpmJsFetch;
