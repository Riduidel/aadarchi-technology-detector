import { type Browser } from "playwright";
import parseURL from "./parseURL";

/**
 * Crawl a site and return a list of github repositories
 * @param site : a name to recognize the site
 * @param pages  : a list of pages to crawl
 * @param selector : a selector to specify of to fetch github repos
 * @param browser : a playwright browser
 * @returns a list of github repositories
 */
export const crawlSite = async (
  site: string,
  pages: string[],
  selector: string,
  browser: Browser
) => {
  console.log(`🔮 \x1b[1m\x1b[4m\x1b[49m${site}\x1b[0m`);
  const listRepositories: string[] = [];

  for (let i = 0; i < pages.length; i++) {
    console.log(`  🕵️  ${pages[i]}`);
    listRepositories.push(...(await parseURL(pages[i], selector, browser)));
  }
  return Array.from(new Set(listRepositories)).sort();
};

export default crawlSite;
