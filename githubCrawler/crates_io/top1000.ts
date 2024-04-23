import { Database } from "bun:sqlite";
import { Artifact } from "../artifact.type";
import { apiToDTO } from "./dto";
import { Registry } from "./types";

const fetchTop1000 = async (db: Database): Promise<Artifact.Root> => {
  const results: Registry.queryResult[] = [];
  const top1000downloads = db
    .query(
      `
      SELECT crates.id, crates.name, crates.description, crate_downloads.downloads
      FROM crate_downloads 
      INNER JOIN crates ON crate_downloads.crate_id = crates.id
      ORDER BY dl DESC LIMIT 1000
    `
    )
    .all();
  top1000downloads.forEach((row: unknown) => {
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

export default fetchTop1000;
