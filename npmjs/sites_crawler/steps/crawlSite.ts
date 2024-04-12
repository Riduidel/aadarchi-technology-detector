import { type Browser } from "playwright";
import parseURL from "./parseURL";
import getPackageJson from "./getPackageJson";

/**
 * Crawl a site and return a list of npm packages
 * @param site : a name to recognize the site
 * @param pages  : a list of pages to crawl
 * @param selector : a selector to specify of to fetch github repos
 * @param browser : a playwright browser
 * @returns a list of npm packages
 */
export const crawlSite = async (
  site: string,
  pages: string[],
  selector: string,
  browser: Browser
) => {
  console.log(`🔮 \x1b[1m\x1b[4m\x1b[49m${site}\x1b[0m`);
  const listLibrairies: string[] = [];

  for (let i = 0; i < pages.length; i++) {
    console.log(`  🕵️  ${pages[i]}`);
    const repos = await parseURL(pages[i], selector, browser);
    for (let r = 0; r < repos.length; r++) {
      console.log(`    🎣 ${repos[r]}`);
      const pkg = await getPackageJson(repos[r]);
      ["dependencies", "devDependencies", "peerDependencies"].forEach(
        (key: string) => {
          if (pkg[key]) listLibrairies.push(...Object.keys(pkg[key]));
        }
      );
    }
  }
  return Array.from(new Set(listLibrairies)).sort();
};

export default crawlSite;
