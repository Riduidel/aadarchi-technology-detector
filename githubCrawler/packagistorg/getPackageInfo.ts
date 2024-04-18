import { Artifact } from "../artifact.type";
import { apiToDTO } from "./dto";

const getPackageInfo = async (pkgName: string): Promise<Artifact.Root> => {
  console.log(`🎣 https://packagist.org/packages/${pkgName}.json`);
  const response = await fetch(
    `https://packagist.org/packages/${pkgName}.json`
  );
  if (response.status !== 200)
    throw new Error(`Could not find package ${pkgName}`);
  const json = await response.json();
  if (!json || !json.package) throw new Error(`Could not find json ${pkgName}`);
  return apiToDTO(json.package);
};

export default getPackageInfo;
