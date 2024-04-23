import { Database } from "bun:sqlite";
import generateDB from "./generateDB";
import fetchTop1000 from "./top1000";
import getBulkPackagesInfo from "./getBulkPackagesInfo";

const CratesIoFetch = async (
  sbomPackages: string[],
  useCache: boolean = false
) => {
  console.log("🔍 Analyse crates.io 🦀");
  if (!useCache) await generateDB();
  const db = new Database("tmp/cratesio.sqlite", { readonly: true });
  console.log("  🏅 Top 1000");
  const top1000 = await fetchTop1000(db);
  console.log(`  📋 SBOM ${sbomPackages.length} packages`);
  const sboms = await getBulkPackagesInfo(sbomPackages, db);
  db.close();
  return {
    ...top1000,
    ...sboms,
  };
};

export default CratesIoFetch;
