import { loadJson, saveJson } from "../utils";
import getBulkPackagesInfo from "./getBulkPackagesInfo";
import top1000 from "./top1000";

const PackagistOrgFetch = async (
  sbomPackages: string[],
  useCache: boolean = false
) => {
  console.log("🔍 Top 1000 PHP packages 🐘");
  let phpPackages;
  if (useCache) {
    phpPackages = await loadJson("tmp/phpPackages.json");
  } else {
    phpPackages = await top1000();
    saveJson("tmp/phpPackages.json", phpPackages);
  }
  return {
    ...phpPackages,
    ...(await getBulkPackagesInfo(sbomPackages)),
  };
};

export default PackagistOrgFetch;
