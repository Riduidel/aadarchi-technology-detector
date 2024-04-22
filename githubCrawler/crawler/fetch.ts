import {
  cleanLibraries,
  getLibrairiesFromSBOM,
  sortLibrairiesByLanguages,
} from "./sbom.utils";
import { loadJson, saveJson } from "../utils";
import getGithubRepositories from "./getGithubRepositories";
import { getSBOM } from "./getSBOM";
import sites from "./sites.json";

const fetchSBOM = async (useCache: boolean) => {
  console.log("🔍 Analyse some sites to fetch most used repositories");
  let githubRepositories;
  if (useCache) {
    githubRepositories = await loadJson("tmp/githubRepositories.json");
  } else {
    githubRepositories = await getGithubRepositories(sites);
    saveJson("tmp/githubRepositories.json", githubRepositories);
  }
  console.log(`🏴‍☠️ found ${githubRepositories.length} repositories`);

  let sbomLibs;
  if (useCache) {
    sbomLibs = await loadJson("tmp/sbom-libs.json");
  } else {
    const promises: Promise<string[]>[] = [];
    githubRepositories.forEach((repo: string) => {
      promises.push(
        new Promise(async (resolve) => {
          // console.log(`📂 Analyse ${repo}`);
          const sbom = await getSBOM(repo);
          if (sbom) resolve(getLibrairiesFromSBOM(sbom));
          else resolve([]);
        })
      );
    });

    sbomLibs = cleanLibraries(
      Array.from(
        new Set(
          (await Promise.all(promises)).reduce((acc, val) => {
            return [...acc, ...val];
          }, [])
        )
      )
    ).sort();
    saveJson("tmp/sbom-libs.json", sbomLibs);
  }

  console.log(`🔍 Sorting ${sbomLibs.length} libraries`);

  return sortLibrairiesByLanguages(sbomLibs);
};

export default fetchSBOM;
