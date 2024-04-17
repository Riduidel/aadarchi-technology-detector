import { Registry } from "../types";

const getPackageInfo = async (pkgName: string): Promise<Registry.Package> => {
  const response = await fetch(`https://repo.packagist.org/p2/${pkgName}.json`);
  if (response.status !== 200)
    throw new Error(`Could not find package ${pkgName}`);
  const json = await response.json();
  if (!json || !json.packages)
    throw new Error(`Could not find package ${pkgName}`);

  const pack = json.packages[pkgName];
  if (!pack) throw new Error(`Could not find package ${pkgName}}`);
  return pack[0];
};

export default getPackageInfo;
