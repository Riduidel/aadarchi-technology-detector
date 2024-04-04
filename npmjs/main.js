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
const responseToJSON = async (response) => response.json();

/**
 * transform a list of promises responses to a list of json objects
 * @param {response} responses a list of promises responses
 * @returns a list of JSON objects
 */
const responsesToJSON = (responses) =>
  Promise.all(responses.map(responseToJSON));

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
const formatVersions = (versions) => {
  if (!versions) return {};

  const transformedData = {};

  for (const version in versions) {
    transformedData[version] = {
      usage: NaN, // Placeholder
      date: null, // Placeholder
      users: NaN, // Placeholder
    };
  }

  return transformedData;
};

/**
 * Format a package
 * @param {Object} package the package to format
 * @param {string} package.name the name of the package
 * @param {string} package.description the description of the package
 * @param {string} package.license the license of the package
 * @param {string[]} package.keywords the keywords of the package
 * @param {users} package.users the users of the package
 * @param {number} package.downloads the downloads of the package
 * @param {versions} package.versions the versions of the package
 * @returns a formatted package
 */
const formatPackage = ({
  name,
  description,
  license,
  keywords,
  users,
  downloads,
  versions,
}) => {
  if (!name) return;

  try {
    return {
      coordinates: name,
      name,
      description,
      license,
      tags: keywords,
      ranking: 0, // à chercher,
      users: Object.keys(users).length,
      downloads,
      repositories: [], // ???
      versions: formatVersions(versions),
    };
  } catch {
    console.error("Could not format package", name);
  }
};

/**
 * Build the urls to fetch the packages
 * @param {string[]} packages a list of packages
 * @returns a list of urls to fetch
 */
const buildUrlsToFetch = (packages) =>
  packages.map((package) => `https://registry.npmjs.com/${package}`);

/**
 * Fetch the packages
 * @param {string[]} urls a list of urls to fetch
 * @returns a list of packages
 */
const fetchPackages = (urls) =>
  Promise.all(urls.map((url) => fetch(url)))
    .then(responsesToJSON)
    .catch((error) => console.error("Error while fetching packages:", error));

/**
 * Build the artifacts
 * @param {Object[]} packages a list of packages
 * @returns a list of artifacts
 */
const buildArtifacts = (packages) => packages.map((pkg) => formatPackage(pkg));

const main = async () => {
  const urls = buildUrlsToFetch(stub);

  const fetchedPackages = await fetchPackages(urls);
  const filteredPackages = fetchedPackages.filter((pkg) => !!pkg.name);

  const artifacts = buildArtifacts(filteredPackages);

  console.log(JSON.stringify(artifacts));
};

// In order to test, you can run the following command:
// node main.js > data.json
// This will create a file named data.json containing the output of the script
main();
