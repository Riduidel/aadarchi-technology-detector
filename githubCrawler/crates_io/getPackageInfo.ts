import { Database } from "bun:sqlite";
import { Artifact } from "../artifact.type";
import { apiToDTO } from "./dto";
import { Registry } from "./types";

const getPackageInfo = async (
  packageName: string,
  db: Database
): Promise<Artifact.Root> => {
  const query = db
    .query(
      `
        SELECT crates.id, crates.name, crates.description, crate_downloads.downloads
        FROM crate_downloads 
        INNER JOIN crates ON crate_downloads.crate_id = crates.id
        WHERE crates.name = $name
    `
    )
    .get({ $name: packageName });
  const keywords = db
    .query(
      `
        SELECT keywords.keyword
        FROM keywords
        INNER JOIN crates_keywords ON keywords.id = crates_keywords.keyword_id
        WHERE crates_keywords.crate_id = $id
    `
    )
    .all({ $id: (query as Registry.queryResult).id ?? "" })
    .flatMap((r) => (r as Registry.queryResult).keyword);
  return apiToDTO({
    name: (query as Registry.queryResult).name,
    description: (query as Registry.queryResult).description,
    dl: (query as Registry.queryResult).dl,
    keywords,
  });
};

export default getPackageInfo;
