import { Artifact } from "../artifact.type";
import { Registry } from "./types";

export const apiToDTO = (
  registryPackage: Registry.Package,
  downloads: number
): Artifact.Root => {
  const { name, description, license, keywords, users } = registryPackage;

  return {
    [`npm:${name}`]: {
      coordinates: `npm:${name}`,
      name,
      description,
      license: license ? [license] : [],
      tags: keywords,
      ranking: "#NA",
      users: users ? Object.keys(users)?.length : 0,
      downloads,
      repositories: ["npmjs.org"],
      categories: [],
    },
  };
};
