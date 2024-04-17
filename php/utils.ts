import { Artifact, Registry } from "./types";

export const saveJson = async (name: string, json) =>
  Bun.write(name, JSON.stringify(json));

export const apiToDTO = (
  registryPackage: Registry.Package,
  downloads: number
): Artifact.Package => {
  const { name, description, license, keywords } = registryPackage;
  return {
    coordinates: name,
    name,
    description,
    license: license ?? [],
    tags: keywords,
    downloads,
  };
};
