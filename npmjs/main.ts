import sites from "./sites_crawler/sites.json";
import {
  getDownloadForPackagesNotScoped,
  getDownloadForPackagesScoped,
  getPackagesDetail,
  getPopularPackages,
} from "./npmJS";
import { processBulk, saveAsJSONFile, downloadLog, detailLog } from "./utils";
import getCrawledPackages from "./sites_crawler/getCrawledPackages";

const fromFile = false;

const main = async () => {
  /**
   * Top 1000 most popular packages downloaded last month in NPMJS
   * The result is format is the target format (detailled)
   */
  const popularPackages = await getPopularPackages();

  /**
   * List of all packages found in codebases repo
   * The result is not detailled
   */
  const crawledPackages: string[] = await getCrawledPackages(fromFile, sites);

  /** List of codebases packages not in Top1000 */
  const packageNameList = crawledPackages.filter(
    (name: string) => !popularPackages.hasOwnProperty(name)
  );

  const otherPackagesWithDetail = await processBulk({
    items: packageNameList,
    nb: 100,
    process: getPackagesDetail,
    log: detailLog,
  });

  const allPackages = {
    ...popularPackages,
    ...otherPackagesWithDetail,
  };

  /** List of all packages we have to fetch the download counts */
  const allPackagesList = Object.values(allPackages);

  /**
   * We split the packages in 2 parts : scoped pacgakes, with format @packagename/scope
   * and not scoped with format packageName
   *
   * This split is done to use call npmjs api in bulk moden which is limited to 128 packages for NOT scoped package
   */

  const scopedPackages = allPackagesList.filter(({ name }) =>
    name.includes("@")
  );
  const notScopedPackages = allPackagesList.filter(
    ({ name }) => !name.includes("@")
  );

  const downloadForPackagesNotscoped = await processBulk({
    items: notScopedPackages,
    nb: 127,
    process: getDownloadForPackagesNotScoped,
    log: downloadLog,
  });

  const downloadForPackagesScoped = await processBulk({
    items: scopedPackages,
    nb: 100,
    process: getDownloadForPackagesScoped,
    log: downloadLog,
  });

  const downloadForAllPackages = {
    ...downloadForPackagesNotscoped,
    ...downloadForPackagesScoped,
  };

  Object.values(allPackages).forEach(
    (packageDetail) =>
      (packageDetail.downloads =
        downloadForAllPackages[packageDetail.name] ?? 0)
  );

  const sortedPackages = (Object.keys(allPackages) as Array<string>)
    .sort()
    .reduce((r: any, k: string) => ((r[k] = allPackages[k]), r), {});

  saveAsJSONFile("packagesWithCounters", downloadForAllPackages);
  saveAsJSONFile("artifact", sortedPackages);

  const OK = "\x1b[32mOK\x1b[0m";
  const KO = "\x1b[33mKO\x1b[0m";

  console.table({
    "Top 1000 Most Popular in NPMJS": {
      status: Object.keys(popularPackages).length === 1000 ? OK : KO,
      value: Object.keys(popularPackages).length,
      detail: "",
    },
    "Crawled sites": {
      status: Object.keys(crawledPackages).length > 0 ? OK : KO,
      value: Object.keys(crawledPackages).length,
      detail: Object.keys(sites).join(","),
    },
    "Get downloads on Scoped Packages": {
      status: Object.keys(downloadForPackagesScoped).length > 0 ? OK : KO,
      value: Object.keys(downloadForPackagesScoped).length,
      detail: "",
    },
    "Get downloads on Not Scoped Packages": {
      status: Object.keys(downloadForPackagesNotscoped).length > 0 ? OK : KO,
      value: Object.keys(downloadForPackagesNotscoped).length,
      detail: "",
    },
  });
};

// In order to test, you can run the following command:
// node main.js > data.json
// This will create a file named data.json containing the output of the script
main();
