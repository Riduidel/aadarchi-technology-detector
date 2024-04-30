import { chromium } from "playwright";
import crawlSite from "./steps/crawlSite";
import { Sites } from "./types";

export const getGithubRepositories = async (sites: Sites) => {
  const browser = await chromium.launch({
    headless: true,
  });
  const listRepositories: string[] = [];

  for (let site in sites) {
    listRepositories.push(
      ...(await crawlSite(
        site,
        sites[site as keyof typeof sites].pages,
        sites[site as keyof typeof sites].selector,
        browser
      ))
    );
  }

  await browser.close();
  return Array.from(new Set(listRepositories)).sort();
};

export default getGithubRepositories;
