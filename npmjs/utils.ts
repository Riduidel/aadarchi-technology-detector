import { Artifact } from "./types";

export const responseToJSON = async (response: Response) => {
  if (response.status !== 200) {
    throw new Error(`${response.status} - ${response.statusText}`);
  }
  return response.json();
};

export const responsesToJSON = <T>(responses: Response[]) =>
  Promise.all<T>(
    responses.filter((response) => response.status === 200).map(responseToJSON)
  );

export const saveAsJSONFile = (filename: string, json: object) =>
  Bun.write(`./output-files/${filename}.json`, JSON.stringify(json));

export const processBulk = async <T, P>(props: {
  items: P[];
  nb: number;
  process: (subItems: P[]) => Promise<T>;
  log: (subItems: P[]) => string;
}) => {
  const { items, log, nb, process } = props;
  const copiedItems = [...items];
  let result: T = {} as any;

  do {
    const subItems = copiedItems.splice(0, nb);

    log && console.log(log(subItems));

    const processResult = await process(subItems);
    result = {
      ...result,
      ...processResult,
    };
  } while (copiedItems.length !== 0);

  return result;
};

export const detailLog = (packages: string[]) =>
  `🔮 getting package detail for ${packages[0]} until ${
    packages[packages.length - 1]
  }`;

export const downloadLog = (packages: Artifact.Package[]) =>
  `🔮 getting mensual download count for ${packages[0].name} until ${
    packages[packages.length - 1].name
  }`;
