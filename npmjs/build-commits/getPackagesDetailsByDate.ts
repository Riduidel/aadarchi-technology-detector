import { format } from "date-fns";

interface DataToFetch {
  range: string;
  urlsToFetch: string[];
}

interface ScopedAndNotScopedPackages {
  scopedPackages: string[];
  notScopedPackages: string[];
}

type UrlList = string[];
type BulkPackageList = string[][];

type TransformedMonthData = [string, number][];
type ProcessedMonthData = { [month: string]: TransformedMonthData };

type Downloads = { downloads: number; day: string };
type PackageData = {
  downloads: Downloads[];
  package: string;
  start: string;
  end: string;
};

type ResponseData = {
  [pkg: string]: PackageData;
};

const USE_CACHE = true;
const RETRY_INTERVAL = 31 * 60 * 1000;
const MAX_RETRY_ATTEMPTS = 5;

const packagesByDate = await Bun.file("./build-commits/pkgByDate.json").json();

const buildUrlList = (packages: string[], range: string): UrlList => {
  const bulkPackageList = buildBulkPackageList(packages);

  return bulkPackageList.map(
    (pkgList) =>
      `https://api.npmjs.org/downloads/range/${range}/${pkgList.join(",")}`
  );
};

const buildBulkPackageList = (packages: string[]): BulkPackageList => {
  const bulkPackages = [];
  let packageList = [];

  const filteredNotScopedPackages =
    filterScopedAndNotScopedPackages(packages).notScopedPackages;

  for (const pkg of filteredNotScopedPackages) {
    packageList.push(pkg);

    if (packageList.length === 128) {
      bulkPackages.push(packageList);
      packageList = [];
    }
  }

  if (packageList.length > 0) {
    bulkPackages.push(packageList);
  }

  return bulkPackages;
};

const buildDataToFetch = (
  packagesByDate: Record<string, string[]>
): DataToFetch[] => {
  const dataToFetch = [];

  for (const [date, packages] of Object.entries(packagesByDate)) {
    const pkg = packages as string[];

    const range = formatDateRange(date);
    const urlsToFetch = buildUrlList(pkg, range);

    dataToFetch.push({ range, urlsToFetch });
  }

  Bun.write("./build-commits/dataToFetch.json", JSON.stringify(dataToFetch));

  return dataToFetch;
};

const formatDateRange = (date: string): string => {
  const d = new Date(date);
  const firstDay = new Date(d.getFullYear(), d.getMonth(), 1);
  const lastDay = new Date(d.getFullYear(), d.getMonth() + 1, 0);

  return `${format(firstDay, "yyyy-MM-dd")}:${format(lastDay, "yyyy-MM-dd")}`;
};

const filterScopedAndNotScopedPackages = (
  packages: string[]
): ScopedAndNotScopedPackages => {
  const scopedPackages = packages.filter((name) => name.includes("@"));
  const notScopedPackages = packages.filter((name) => !name.includes("@"));

  return { scopedPackages, notScopedPackages };
};

const getCachedDataToFetch = async (): Promise<DataToFetch[]> => {
  return Bun.file("./build-commits/dataToFetch.json").json();
};

const transformToMonthData = (data: ResponseData): TransformedMonthData => {
  const monthDataArray: TransformedMonthData = [];

  Object.entries(data).forEach(([pkgName, value]) => {
    const downloads = value.downloads.reduce(
      (acc, curr) => acc + curr.downloads,
      0
    );

    if (downloads > 0) {
      monthDataArray.push([pkgName, downloads]);
    }
  });

  return monthDataArray;
};

const fetchAll = async (urls: string[]): Promise<Response[]> =>
  Promise.all(urls.map((url) => fetch(url)));

const fetchMonthData = async (
  month: DataToFetch
): Promise<TransformedMonthData> => {
  const responses = await fetchAll(month.urlsToFetch);
  const monthData: TransformedMonthData = [];

  for (const response of responses) {
    const data: ResponseData = await response.json();

    const dataToMerge = transformToMonthData(data);

    monthData.push(...dataToMerge);
  }

  return monthData;
};

const processMonth = async (
  range: DataToFetch
): Promise<ProcessedMonthData> => {
  const splittedMonthRange = range.range?.split(":")[0].slice(0, -3);
  const monthData = await fetchMonthData(range);

  if (USE_CACHE) {
    Bun.write(
      `./build-commits/history/${splittedMonthRange}.json`,
      JSON.stringify(monthData)
    );
  }

  return { [splittedMonthRange]: monthData };
};

const fetchData = async (
  dataToFetchArray: DataToFetch[]
): Promise<ProcessedMonthData[]> =>
  Promise.all(dataToFetchArray.map(processMonth));

const retryFetch = async (
  fn: () => Promise<ProcessedMonthData[]>,
  retryInterval: number,
  maxRetries: number
): Promise<ProcessedMonthData[] | undefined> => {
  let retries = 0;
  let result;

  while (retries < maxRetries) {
    try {
      result = await fn();
      break;
    } catch (error) {
      console.error(
        `Error fetching data. Retrying in ${retryInterval / 60000} minutes...`
      );

      for (let i = retryInterval / 60000; i > 0; i--) {
        console.info(`Retrying in ${i} minute(s)...`);
        await new Promise((resolve) => setTimeout(resolve, 60000));
      }

      retries++;
    }
  }

  if (retries === maxRetries) {
    throw new Error(`Failed after ${retries} retries.`);
  }

  return result;
};

const main = async () => {
  const dataToFetchArray = USE_CACHE
    ? buildDataToFetch(packagesByDate)
    : await getCachedDataToFetch();

  await retryFetch(
    () => fetchData(dataToFetchArray),
    RETRY_INTERVAL,
    MAX_RETRY_ATTEMPTS
  );
};

main();
