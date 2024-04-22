import { Database } from "bun:sqlite";
import { Artifact } from "../artifact.type";
import getPackageInfo from "./getPackageInfo";

const getBulkPackagesInfo = async (packageNames: string[], db: Database) => {
  const promises: Promise<Artifact.Root>[] = [];
  packageNames.forEach((name) => {
    promises.push(
      new Promise(async (resolve) => {
        try {
          const pkgInfo = await getPackageInfo(name, db);
          if (pkgInfo) resolve(pkgInfo);
        } catch (e) {
          resolve({
            [`rust:${name}`]: { name },
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
