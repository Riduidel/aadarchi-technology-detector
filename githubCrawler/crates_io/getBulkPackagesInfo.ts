import { Database } from "bun:sqlite";
import { Registry } from "./types";
import { apiToDTO } from "./dto";
import { Artifact } from "../artifact.type";

const getBulkPackagesInfo = async (
  packageNames: string[],
  db: Database
): Promise<Artifact.Root> => {
  const results: Registry.queryResult[] = [];
  const res = db
    .query(
      `
        SELECT crates.id, crates.name, crates.description, CAST(crate_downloads.downloads as integer) as dl
        FROM crate_downloads 
        INNER JOIN crates ON crate_downloads.crate_id = crates.id
        WHERE crates.name IN ($name)
    `
    )
    .all({ $name: packageNames.join(",") });
  res.forEach((row: unknown) => {
    const keywords = db
      .query(
        `
      SELECT keywords.keyword
      FROM keywords
      INNER JOIN crates_keywords ON keywords.id = crates_keywords.keyword_id
      WHERE crates_keywords.crate_id = $id
      `
      )
      .all({ $id: (row as Registry.queryResult).id ?? "" })
      .flatMap((r) => (r as Registry.queryResult).keyword);

    results.push({
      name: (row as Registry.queryResult).name,
      description: (row as Registry.queryResult).description,
      dl: (row as Registry.queryResult).dl,
      keywords,
    });
  });
  return results.map(apiToDTO).reduce((acc, val) => {
    return { ...acc, ...val };
  }, {});
};

export default getBulkPackagesInfo;
