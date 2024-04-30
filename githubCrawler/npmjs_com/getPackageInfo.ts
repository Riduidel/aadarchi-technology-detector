import { Artifact } from "../artifact.type";
import { apiToDTO } from "./dto";
import { getDownloadCount } from "./getDownloadCount";

export const getPackageInfo = async (
  packageName: string,
  withDownloadCounter: boolean = false
): Promise<Artifact.Root> => {
  // console.log(`🎣 https://registry.npmjs.com/${packageName}`);
  let res: Artifact.Root = {};
  try {
    const response = await fetch(`https://registry.npmjs.com/${packageName}`);
    if (response.status !== 200)
      throw new Error(`Could not find package ${packageName}`);
    const json = await response.json();
    let downloads = 0;
    if (withDownloadCounter) {
      downloads =
        (await getDownloadCount([packageName]))[`npm:${packageName}`] || 0;
    }
    res = apiToDTO(json, downloads);
  } catch (e) {}
  return res;
};
