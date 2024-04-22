import { Artifact } from "../artifact.type";
import getPackageInfo from "./getPackageInfo";
import { Registry } from "./types";

export const top1000 = async (): Promise<Artifact.Root> => {
  let pages: string[] = [];
  for (let i = 1; i < 10; i++)
    pages.push(
      `https://packagist.org/explore/popular.json?per_page=100&page=${i}`
    );
  const responses = await Promise.all(pages.map((url) => fetch(url)));
  const jsons = await Promise.all(responses.map((r) => r.json()));
  const promises: Promise<Artifact.Root>[] = [];
  jsons.forEach(({ packages }) =>
    (packages as Registry.Popular[]).map(({ name, description }) => {
      promises.push(
        new Promise(async (resolve) => {
          const pkgInfo = await getPackageInfo(name);
          if (pkgInfo) resolve(pkgInfo);
          else
            resolve({
              [`composer:${name}`]: { name, description },
            } as Artifact.Root);
        })
      );
    })
  );
  const packagesInfos = await Promise.all(promises);
  return packagesInfos.reduce((acc, val) => {
    return { ...acc, ...val };
  }, {});
};

export default top1000;
