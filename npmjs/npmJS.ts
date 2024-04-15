import { Artifact, Registry } from "./types";
import { responseToJSON, responsesToJSON } from "./utils";

type GetDownloadFor = (packageList: Artifact.Package[]) => Promise<{
  [k: string]: number;
}>;

/**
 * Get the last month top 1000 most downloaded packages from npmjs
 * The result is formatted to our own artifcat format
 */
export const getPopularPackages = async () => {
  const popularPackages = await fetchTop1000();

  return Object.fromEntries(
    Object.entries(popularPackages).map(([name, packageDetail]) => [
      name,
      formatPackage(packageDetail),
    ])
  );
};

/**
 * Get the detail of each package given in param
 * The result is formatted to our own artifcat format
 */
export const getPackagesDetail = async (packageList: string[]) =>
  await Promise.all(
    packageList.map(async (packageCode) => {
      await Bun.sleep(10000);
      return fetch(`https://registry.npmjs.com/${packageCode}`);
    })
  )
    .then(responsesToJSON<Registry.Package>)
    .then((responses) => {
      return Object.fromEntries(
        Object.values(responses).map((packageDetail) => [
          packageDetail.name,
          formatPackage(packageDetail),
        ])
      );
    });

export const getDownloadForPackagesNotScoped: GetDownloadFor = async (
  packageList
) => {
  const bulkPackagesQueryParam = packageList.map(({ name }) => name).join(",");
  const url = `https://api.npmjs.org/downloads/point/last-month/${bulkPackagesQueryParam}`;

  const promises = [fetch(url)];
  return fetch(url)
    .then(
      (response) =>
        responseToJSON(response) as Promise<{
          [packageName: string]: Registry.DownloadsCounter;
        }>
    )
    .then((response) => {
      return Object.fromEntries(
        Object.values(response).map((counterDetail) => [
          counterDetail.package,
          counterDetail.downloads,
        ])
      );
    })
    .catch((err) => {
      console.log("url", url);
      console.log(err);
      return {};
    });
};

export const getDownloadForPackagesScoped: GetDownloadFor = async (
  packageList
) => {
  const promises = packageList.map((packageDetail) =>
    fetch(
      `https://api.npmjs.org/downloads/point/last-month/${packageDetail.name}`
    )
  );

  return Promise.all(promises)
    .then(responsesToJSON<Registry.DownloadsCounter>)
    .then((responses) => {
      return Object.fromEntries(
        Object.values(responses).map((counterDetail) => [
          counterDetail.package,
          counterDetail.downloads,
        ])
      );
    })
    .catch((err) => {
      console.log(err);
      return {};
    });
};

const fetchTop1000 = async () => {
  return Promise.all(
    [
      `https://registry.npmjs.com/-/v1/search?text=not:unstable&popularity=1.0&from=${0}&size=${250}`,
      `https://registry.npmjs.com/-/v1/search?text=not:unstable&popularity=1.0&from=${251}&size=${250}`,
      `https://registry.npmjs.com/-/v1/search?text=not:unstable&popularity=1.0&from=${501}&size=${250}`,
      `https://registry.npmjs.com/-/v1/search?text=not:unstable&popularity=1.0&from=${751}&size=${250}`,
    ].map((url) => fetch(url))
  )
    .then(responsesToJSON<Registry.Popular>)
    .then((responses) =>
      responses.reduce<{ [packageName: string]: Registry.Package }>(
        (acc, cur) => ({
          ...acc,
          ...Object.fromEntries(
            cur.objects.map(({ package: pakageDetail }) => [
              pakageDetail.name,
              pakageDetail,
            ])
          ),
        }),
        {}
      )
    );
};

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
    license: license ? [license] : [],
    tags: keywords,
    ranking: "#NA",
    users: users ? Object.keys(users)?.length : 0,
    downloads: 0,
    repositories: repository?.type ? [repository.type] : [],
    categories: [],
  };
};
