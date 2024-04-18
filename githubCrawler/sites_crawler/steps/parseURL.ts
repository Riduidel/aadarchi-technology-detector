import type { Browser } from "playwright";

/**
 * ParseURL - Parse URL and return a list of github repositories urls
 * @param url: string - URL to parse
 * @param selector: string - Selector to use for fetching urls
 * @param browser: Browser - a playwright browser instance
 * @returns a list of github repository urlss
 */
export const parseURL = async (
  url: string,
  selector: string,
  browser: Browser
): Promise<string[]> => {
  const [sel, manip] = selector.split("|>").map((s) => s.trim());
  const [manipAction, ...manipValue] = manip.split(":").map((s) => s.trim());
  const page = await browser.newPage();
  await page.goto(url);
  await page.waitForSelector(sel);
  await page.waitForTimeout(1000);
  const repos = (
    await page.evaluate(
      ([sel, manip]) => {
        return Array.from(document.querySelectorAll(sel)).map((el) => {
          if (manip && manip === "parent")
            return el.parentElement?.getAttribute("href");
          return (el as HTMLElement).getAttribute("href");
        });
      },
      [sel, manip]
    )
  ).map((r) => {
    if (manipAction === "prepend") return `${manipValue.join(":")}${r}`;
    return r;
  });
  const repositories: string[] = [];
  repos.forEach((r) => {
    if (r) repositories.push(r);
  });
  return repositories;
};

export default parseURL;
