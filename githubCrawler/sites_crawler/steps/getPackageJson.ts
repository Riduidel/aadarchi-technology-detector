/**
 * GetPackageJson - get package.json content from a repository
 * @param repoUrl : a repository url
 * @returns a package.json content
 */
export const getPackageJson = async (repoUrl: string) => {
  const newRepoUrl = repoUrl
    .replace("https://github.com", "https://raw.githubusercontent.com")
    .replace("#readme", "");
  let response = await fetch(`${newRepoUrl}/main/package.json`);
  if (!response.ok) {
    // try with old master branch
    response = await fetch(`${newRepoUrl}/master/package.json`);
  }
  if (!response.ok) return {};
  return await response.json();
};

export default getPackageJson;
