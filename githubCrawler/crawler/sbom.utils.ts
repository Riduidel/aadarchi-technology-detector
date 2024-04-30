import { SBOM, SBOMLanguages, SBOMLibs } from "./sbom.types";

export const getLibrairiesFromSBOM = (sboms: SBOM[] | null[]) => {
  // the first one is the current repository
  if (sboms == null || sboms?.length === 0) return [];
  return Array.from(new Set(sboms.slice(1).map((sb) => sb.name))).sort();
};

export const sortLibrairiesByLanguages = (
  librairies: string[]
): SBOMLanguages => {
  const libs: SBOMLibs = {
    npm: [],
    php: [],
    rust: [],
  };
  librairies.forEach((lib) => {
    if (lib.startsWith("npm:")) libs.npm.push(lib.replace("npm:", ""));
    else if (lib.startsWith("composer:"))
      libs.php.push(lib.replace("composer:", ""));
    else if (lib.startsWith("rust:")) libs.rust.push(lib.replace("rust:", ""));
  });
  return libs;
};

export const cleanLibraries = (libraries: string[]) =>
  libraries.filter(
    (lib) => !lib.startsWith("actions:") && lib.indexOf("../") === -1
  );
