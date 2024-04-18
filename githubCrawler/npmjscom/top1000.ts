import { Artifact } from "../artifact.type";
import { getPackageInfo } from "./getPackageInfo";
import { Registry } from "./types";

export const top1000 = async (): Promise<Artifact.Root[]> => {
  let pages: string[] = [];
  for (let i = 0; i < 4; i++)
    pages.push(
      `https://registry.npmjs.com/-/v1/search?text=not:unstable&popularity=1.0&from=${
        i * 250
      }&size=250`
    );
  const responses = await Promise.all(pages.map((url) => fetch(url)));
  const jsons = await Promise.all(responses.map((r) => r.json()));
  return await Promise.all(
    jsons.flatMap((pkgs) =>
      (pkgs as Registry.Popular).objects.map(async (pkg) => {
        const pkgInfo = await getPackageInfo(pkg.package.name);
        if (pkgInfo) return pkgInfo;
        return {
          coordinates: pkg.package.name,
          name: pkg.package.name,
          description: pkg.package.description || "",
        };
      })
    )
  );
};
