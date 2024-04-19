/**
 * In order to make this script work, you need to edit the Artifact.Package interface in the types.ts file.
 * You need to add a new field named `time` of type `Record<string, string>`.
 * and then add the property `time` to the returned value of the function `formatPackage` in the main.ts file.
 */

import { format } from "date-fns";
import { Artifact } from "../types";

type Data = Record<string, string[]>;

const packagesFromArtifact = await Bun.file(
  "./output-files/artifact.json"
).json();

const getFormattedFirstDayOfMonth = (stringDate: string): string => {
  const d = new Date(stringDate);

  return format(new Date(d.getFullYear(), d.getMonth(), 1), "yyyy-MM-dd");
};

const getCreationDate = (time: Record<string, string>): string => {
  return getFormattedFirstDayOfMonth(time?.created);
};

const createDatesByMonthSince2015 = () => {
  const dates = [];
  let date = new Date("2015-01-01");

  while (date < new Date()) {
    dates.push(format(date, "yyyy-MM-dd"));
    date.setMonth(date.getMonth() + 1);
  }

  return dates;
};

const createArtifactFileByDate = () => {
  return createDatesByMonthSince2015().reduce((acc, date) => {
    acc[date] = [];
    return acc;
  }, {} as Data);
};

const main = async () => {
  const data = createArtifactFileByDate();

  for (const [_, pkg] of Object.entries(packagesFromArtifact)) {
    const p = pkg as Artifact.Package;

    const date = p.time ? getCreationDate(p.time) : "2015-01-01";

    for (const [d] of Object.entries(data)) {
      if (date <= d) {
        data[d].push(p.name);
      }
    }
  }

  await Bun.write("./pkgByDate.json", JSON.stringify(data));
};

main();
