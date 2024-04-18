import { Artifact } from "../artifact.type";
import { Registry } from "./types";

export const apiToDTO = (
  registryPackage: Registry.Package
): Artifact.Package => {
  const { name, description, license, keywords, users } = registryPackage;

  return {
    coordinates: name,
    name,
    description,
    license: license ? [license] : [],
    tags: keywords,
    ranking: "#NA",
    users: users ? Object.keys(users)?.length : 0,
    downloads: 0,
    repositories: ["npmjs.org"],
    categories: [],
  };
};
