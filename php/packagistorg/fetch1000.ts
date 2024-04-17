import { Registry } from "../types";

export const fetch1000 = async (): Promise<Registry.Popular[]> => {
  let pages: string[] = [];
  for (let i = 1; i < 10; i++)
    pages.push(
      `https://packagist.org/explore/popular.json?per_page=100&page=${i}`
    );
  const responses = await Promise.all(pages.map((url) => fetch(url)));
  const jsons = await Promise.all(responses.map((r) => r.json()));
  return jsons.flatMap(({ packages }) => packages);
};

export default fetch1000;
