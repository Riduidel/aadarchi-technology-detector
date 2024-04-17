/**
 * @author Sylvain Gougouzian
 * @version 1.0
 * @description Analyze Packagist and get the most used packages
 */

import fetch1000 from "./packagistorg/fetch1000";
import getPackageInfo from "./packagistorg/getPackageInfo";
import { apiToDTO, saveJson } from "./utils";

const top1000 = await fetch1000();

saveJson(
  "list.json",
  await Promise.all(
    top1000.map(async ({ name, description, url, downloads }) => {
      const pkgInfo = await getPackageInfo(name);
      if (pkgInfo) return apiToDTO(pkgInfo, downloads);
      return apiToDTO({ name, description, homepage: url }, downloads);
    })
  )
);
