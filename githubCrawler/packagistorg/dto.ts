import { Artifact } from "../artifact.type";
import { Registry } from "./types";

export const apiToDTO = (registryPackage: Registry.Package): Artifact.Root => {
  const { name, description, keywords, downloads, versions } = registryPackage;
  let version: Registry.Version = { license: [] };
  if (versions) version = versions[Object.keys(versions)[0]];
  return {
    [`composer:${name}`]: {
      coordinates: `composer:${name}`,
      name,
      description,
      license: version.license,
      tags: keywords,
      downloads: downloads?.monthly,
      repositories: ["packagist.org"],
    },
  };
};
