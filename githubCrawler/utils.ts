import { Artifact } from "./artifact.type";

export const saveJson = async (name: string, json: any) =>
  Bun.write(name, JSON.stringify(json));

export const loadJson = async (name: string): Promise<any> =>
  await Bun.file(name).json();

export const myFetch = (
  url: string,
  options?: RequestInit
): Promise<Response> =>
  new Promise<Response>(async (resolve) => {
    let bool = false;
    let response: Response = new Response();
    while (!bool) {
      // console.log(`🔍 ${url}`);
      response = await fetch(url, options);
      if (response.status === 200) {
        bool = true;
      } else if (response.status === 404) {
        bool = true;
      } else if (response.status === 429) {
        console.log(
          `⏲️  waiting ${Number(
            response.headers.get("retry-after")
          )}s for ${url}`
        );
        await new Promise((resolve) =>
          setTimeout(
            () => resolve(""),
            Number(response.headers.get("retry-after")) * 1000
          )
        );
      }
    }
    resolve(response);
  });
