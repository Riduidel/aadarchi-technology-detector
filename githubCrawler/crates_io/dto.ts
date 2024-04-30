import { Artifact } from "../artifact.type";
import { Registry } from "./types";

export const apiToDTO = (
  registryPackage: Registry.queryResult
): Artifact.Root => {
  const { name, description, keywords, dl } = registryPackage;

  return {
    [`rust:${name}`]: {
      coordinates: `rust:${name}`,
      name,
      description,
      license: [""],
      tags: keywords,
      downloads: dl,
      repositories: ["crates.io"],
    },
  };
};
