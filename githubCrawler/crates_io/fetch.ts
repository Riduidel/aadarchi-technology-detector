import generateDB from "./generateDB";
import fetchTop1000 from "./top1000";

const CratesIoFetch = async (
  sbomPackages: string[],
  useCache: boolean = false
) => {
  console.log("🔍 Analyse crates.io 🦀");
  if (!useCache) await generateDB();
  console.log("🔍 Top 1000 Rust packages 🦀");
  const top1000 = await fetchTop1000();
  return {
    ...top1000,
    // ...(await getBulkPackagesInfo(sbomPackages)),
  };
};

export default CratesIoFetch;
