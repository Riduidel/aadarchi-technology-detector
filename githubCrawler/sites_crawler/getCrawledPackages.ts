import { saveAsJSONFile } from "../utils";
import crawler from "./steps/crawler";

export const getCrawledPackages = async (useCache: boolean, sites: any) => {
  let packages: string[];
  if (useCache) {
    packages = await Bun.file("./output-files/libs.json").json();
  } else {
    packages = await crawler(sites);
    saveAsJSONFile("libs", packages);
  }
  return packages;
};

export default getCrawledPackages;
