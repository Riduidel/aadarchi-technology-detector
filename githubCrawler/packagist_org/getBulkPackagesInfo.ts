import { Artifact } from "../artifact.type";
import getPackageInfo from "./getPackageInfo";

export const getBulkPackagesInfo = async (packageNames: string[]) => {
  const promises: Promise<Artifact.Root>[] = [];
  packageNames.forEach((name) => {
    promises.push(
      new Promise(async (resolve) => {
        try {
          const pkgInfo = await getPackageInfo(name);
          if (pkgInfo) resolve(pkgInfo);
        } catch (e) {
          resolve({
            [`composer:${name}`]: { name },
          } as Artifact.Root);
        }
      })
    );
  });
  const packagesInfos = await Promise.all(promises);
  return packagesInfos.reduce((acc, val) => {
    return { ...acc, ...val };
  }, {});
};

export default getBulkPackagesInfo;
