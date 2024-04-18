import { Artifact } from "../artifact.type";
import { Registry } from "./types";
import { responseToJSON, responsesToJSON } from "./utils";

type GetDownloadFor = (packageList: Artifact.Package[]) => Promise<{
  [k: string]: number;
}>;

export const getDownloadForPackagesNotScoped: GetDownloadFor = async (
  packageList
) => {
  const bulkPackagesQueryParam = packageList.map(({ name }) => name).join(",");
  const url = `https://api.npmjs.org/downloads/point/last-month/${bulkPackagesQueryParam}`;

  return myFetch(url)
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
    myFetch(
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
