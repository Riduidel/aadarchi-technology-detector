import { myFetch } from "../utils";
import { DownloadCounter, Registry } from "./types";

export const getDownloadCount = async (
  packageNames: string[]
): Promise<DownloadCounter> => {
  const scopedNames: string[] = [];
  const unscopedNames: string[] = [];
  packageNames.forEach((name) =>
    name.startsWith("@") ? scopedNames.push(name) : unscopedNames.push(name)
  );
  const unscopedDownloads: string[][] = [];
  while (unscopedNames.length) {
    unscopedDownloads.push(unscopedNames.splice(0, 100));
  }
  const promises: Promise<DownloadCounter>[] = [];
  promises.push(
    ...unscopedDownloads.flatMap(
      (bulk): Promise<DownloadCounter> =>
        new Promise(async (resolve) => {
          const response = await myFetch(
            `https://api.npmjs.org/downloads/point/last-month/${bulk.join(",")}`
          );
          const json = await response.json();
          const res = [...Object.values(json)]
            .map((val) => ({
              [`npm:${(val as Registry.DownloadsCounter).package}`]: (
                val as Registry.DownloadsCounter
              ).downloads,
            }))
            .reduce((acc, val) => {
              return { ...acc, ...val };
            }, {});
          resolve(res);
        })
    ),
    ...scopedNames.flatMap(
      async (name): Promise<DownloadCounter> =>
        new Promise(async (resolve) => {
          const response = await myFetch(
            `https://api.npmjs.org/downloads/point/last-month/${name}`
          );
          resolve({
            [`npm:${name}`]: (
              (await response.json()) as Registry.DownloadsCounter
            ).downloads,
          });
        })
    )
  );
  return (await Promise.all(promises)).reduce((acc, val) => {
    return { ...acc, ...val };
  }, {});
};
