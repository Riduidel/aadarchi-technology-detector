import { chromium } from "playwright";
import crawlSite from "./crawlSite";

/**
 * Crawler, crawls a website and returns a list of npm packages
 * @param sites a json object with the url of each website and a list of pages to crawl
 * @returns an array of npm packages sorted alphabetically
 */
export const crawler = async (sites: any) => {
  const browser = await chromium.launch({
    headless: true,
  });
  const listLibrairies: string[] = [];

  for (let site in sites) {
    listLibrairies.push(
      ...(await crawlSite(
        site,
        sites[site as keyof typeof sites].pages,
        sites[site as keyof typeof sites].selector,
        browser
      ))
    );
  }

  await browser.close();
  return Array.from(new Set(listLibrairies)).sort();
};

export default crawler;
