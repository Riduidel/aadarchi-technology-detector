import { Artifact } from "../artifact.type";
import { getDownloadCount } from "./getDownloadCount";
import { getPackageInfo } from "./getPackageInfo";
import { Registry } from "./types";

export const top1000 = async (): Promise<Artifact.Root> => {
  let pages: string[] = [];
  for (let i = 0; i < 4; i++)
    pages.push(
      `https://registry.npmjs.com/-/v1/search?text=not:unstable&popularity=1.0&from=${
        i * 250
      }&size=250`
    );
  const responses = await Promise.all(pages.map((url) => fetch(url)));
  const jsons = await Promise.all(responses.map((r) => r.json()));
  const promises: Promise<Artifact.Root>[] = [];
  jsons.forEach(({ objects }) =>
    objects.forEach((pkg: Registry.PopularObject) => {
      promises.push(
        new Promise(async (resolve) => {
          const pkgInfo = await getPackageInfo(pkg.package.name);
          if (pkgInfo) resolve(pkgInfo);
          else
            resolve({
              [`npm:${pkg.package.name}`]: {
                name: pkg.package.name,
                description: pkg.package.description || "",
              },
            } as Artifact.Root);
        })
      );
    })
  );
  const packagesInfos = (await Promise.all(promises)).reduce((acc, val) => {
    return { ...acc, ...val };
  }, {});
  // npmjs doesn't provide the download counter when fetching populars, so we need to add it ourselves
  const packagesNames = [...Object.keys(packagesInfos)].map((n) =>
    n.replace("npm:", "")
  );
  const downloads = await getDownloadCount(packagesNames);
  for (const name in downloads) {
    packagesInfos[name].downloads = downloads[name];
  }
  return packagesInfos;
};
