import { apiToDTO } from "./dto";

export const getPackageInfo = async (packageName: string) => {
  const response = await fetch(`https://registry.npmjs.com/${packageName}`);
  console.log(`https://registry.npmjs.com/${packageName}`);
  if (response.status !== 200)
    throw new Error(`Could not find package ${packageName}`);
  const json = await response.json();
  return apiToDTO(json);
};
