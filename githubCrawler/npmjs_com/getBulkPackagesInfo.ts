import { Artifact } from "../artifact.type";
import { getDownloadCount } from "./getDownloadCount";
import { getPackageInfo } from "./getPackageInfo";
import { GetBulkPackagesOptions } from "./types";

export const getBulkPackagesInfo = async (
  packageNames: string[],
  options: GetBulkPackagesOptions = {
    withDownloads: false,
    withScopedDownloads: false,
  }
): Promise<Artifact.Root> => {
  const { withDownloads, withScopedDownloads } = options;
  const promises: Promise<Artifact.Root>[] = [];
  packageNames.forEach((name) => {
    promises.push(
      new Promise(async (resolve) => {
        const pkgInfo = await getPackageInfo(name);
        if (pkgInfo) resolve(pkgInfo);
        else
          resolve({
            [`npm:${name}`]: {
              name: name,
            },
          } as Artifact.Root);
      })
    );
  });
  const packagesInfos = (await Promise.all(promises)).reduce((acc, val) => {
    return { ...acc, ...val };
  }, {});
  // npmjs doesn't provide the download counter when fetching populars, so we need to add it ourselves
  const packagesNames = [...Object.keys(packagesInfos)].map((n) =>
    n.replace("npm:", "")
  );
  if (withDownloads) {
    const downloads = await getDownloadCount(
      packagesNames,
      withScopedDownloads
    );
    for (const name in downloads) {
      if (packagesInfos[name])
        packagesInfos[name].downloads = downloads[name] ?? 0;
    }
  }
  return packagesInfos;
};

export default getBulkPackagesInfo;
