import { Artifact, Registry } from "./types";
import { writeFile } from "fs";

const artifactFilePathName = "./artifacts.json";

// A list of packages, used to test the script
const stub = [
  "react",
  "moment",
  "redux",
  "lodash",
  "tototototototototo", // does not exist, used to test error handling
  "express",
  "koa",
  "koa-router",
  "koa-bodyparser",
];

/**
 * transform a promise response to a json object
 * @param {response} response a promise response
 * @returns a JSON object
 */
const responseToJSON = async (response: Response) => response.json();

/**
 * transform a list of promises responses to a list of json objects
 * @param {response} responses a list of promises responses
 * @returns a list of JSON objects
 */
const responsesToJSON = <T>(responses: Response[]) =>
  Promise.all<T>(responses.map(responseToJSON));

/**
 * Format the versions of a package
 * @param {versions} versions an object of versions
 * @returns a pseudo list of versions:
 * {
 *   "1.0.0": { usage: NaN, date: null, users: NaN },
 *   "1.0.1": { usage: NaN, date: null, users: NaN },
 *   ...
 * }
 */
const formatVersions = (versions: Registry.Versions, times: Registry.Time) =>
  Object.fromEntries(
    Object.keys(versions).map<[string, Artifact.Version]>((version) => [
      version,
      {
        usages: "#NA",
        date: times?.[version] ?? "",
        users: 0,
        downloads: 0,
      },
    ])
  );

/** Format a package registry to a package artifact */
const formatPackage = (registryPackage: Registry.Package): Artifact.Package => {
  const {
    name,
    description,
    license,
    keywords,
    users,
    versions,
    repository,
    time,
  } = registryPackage;

  return {
    coordinates: name,
    name,
    description,
    license: [license],
    tags: keywords,
    ranking: "#NA",
    users: Object.keys(users).length,
    downloads: 0,
    repositories: repository ? [repository.type] : [],
    categories: [],
    versions: formatVersions(versions, time),
  };
};

/**
 * Build the urls to fetch the packages
 * @param {string[]} packages a list of packages
 * @returns a list of urls to fetch
 */
const buildUrlsToFetch = (packageCodeList: string[]) =>
  packageCodeList.map(
    (packageCode) => `https://registry.npmjs.com/${packageCode}`
  );

/**
 * Fetch the packages
 * @param {string[]} urls a list of urls to fetch
 * @returns a list of packages
 */
const fetchPackages = (urlList: string[]) =>
  Promise.all(urlList.map((url) => fetch(url)))
    .then((responses) =>
      // We use only successfull response
      responsesToJSON<Registry.Package>(
        responses.filter((response) => response.status === 200)
      )
    )
    .catch((error) => console.error("Error while fetching packages:", error));

/**
 * Build the artifacts
 * @param {Object[]} packages a list of packages
 * @returns a list of artifacts
 */
const buildArtifacts = (registeryPackageList: Registry.Package[]) =>
  registeryPackageList.map(formatPackage);

const updateAllDownloadCounters = async (artifactList: Artifact.Package[]) => {
  const responses = await Promise.all(
    artifactList.map(({ name }) =>
      fetch(`https://api.npmjs.org/versions/${name}/last-week`)
    )
  ).then((responses) =>
    // We use only successfull response
    responsesToJSON<Registry.DownloadsCounter>(
      responses.filter((response) => response.status === 200)
    )
  );

  responses.forEach((response, index) => {
    const artifact = artifactList[index];

    Object.entries(response.downloads).forEach(([version, downloadsCount]) => {
      if (artifact.versions?.[version] !== undefined) {
        artifact.versions[version].downloads = downloadsCount;
      }
    });
  });
};

const main = async () => {
  const urls = buildUrlsToFetch(stub);

  const fetchedPackages = await fetchPackages(urls);

  if (fetchedPackages) {
    const artifacts = buildArtifacts(fetchedPackages);
    artifacts && (await updateAllDownloadCounters(artifacts));
    writeFile(artifactFilePathName, JSON.stringify(artifacts), (error) => {
      if (error) {
        console.log("An error has occurred ", error);
        return;
      }
      console.log("Data written successfully to disk");
    });
  } else {
    console.log("Error on fetcinh packages");
  }
};

// In order to test, you can run the following command:
// node main.js > data.json
// This will create a file named data.json containing the output of the script
main();
