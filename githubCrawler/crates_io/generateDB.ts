import { Database } from "bun:sqlite";
import parse from "csv-simple-parser";

type noImportsType = {
  [key: string]: string[];
};

type tupleType = {
  [key: string]: string[];
};

const generateDB = async () => {
  console.log("  🎣 fetch dump file online");
  await Bun.$`wget -O tmp/cratesiodump.tar.gz https://static.crates.io/db-dump.tar.gz`.quiet();
  await Bun.$`cd tmp && rm -rf cratesiodump && mkdir cratesiodump && tar -xzvf cratesiodump.tar.gz -C cratesiodump && rm cratesiodump.tar.gz`.quiet();
  const dir = (await Bun.$`ls -d tmp/cratesiodump/*`.text()).trim();
  await Bun.$`rm cratesio.sqlite`.quiet();
  console.log("  🗄️ Create Database");
  const db = new Database("tmp/cratesio.sqlite", { create: true });
  const tables = {
    crate_downloads: ["crate_id", "downloads"],
    crates_keywords: ["crate_id", "keyword_id"],
    keywords: ["id", "keyword", "crates_cnt", "created_at"],
    crates: [
      "id",
      "name",
      "updated_at",
      "created_at",
      "description",
      "homepage",
      "documentation",
      "readme",
      "textsearchable_index_col",
      "repository",
      "max_upload_size",
      "max_features",
    ],
  };

  const noImports: noImportsType = {
    crates: ["readme"],
  };

  const file = Bun.file("tmp/cratesio.sql");
  const writer = file.writer({ highWaterMark: 1024 * 1024 });

  Object.entries(tables).forEach(([table, columns]) => {
    writer.write(`
CREATE TABLE IF NOT EXISTS ${table} (${columns
      .map((col) => `${col} TEXT`)
      .join(", ")});
DELETE FROM ${table};\n`);
  });

  const promises: Promise<string>[] = [];
  ["crate_downloads", "crates_keywords", "keywords", "crates"].forEach(
    (table) => {
      promises.push(
        new Promise(async (resolve) => {
          console.log(`    📥 Import ${table}: ${dir}/data/${table}.csv`);
          const file = Bun.file(`${dir}/data/${table}.csv`);
          const sql: string[] = ["begin transaction;"];
          const csv = parse(await file.text(), { header: true }) as object[];
          for (const row of csv) {
            const tuple: tupleType = { keys: [], values: [] };
            for (const [key, value] of Object.entries(row)) {
              if (noImports[table]?.includes(key)) {
              } else {
                tuple.keys.push(key);
                tuple.values.push(
                  `"${value
                    .replaceAll('"', '""')
                    .replaceAll("'", "''")
                    .replaceAll("\n", " ")}"`
                );
              }
            }
            sql.push(
              `INSERT INTO ${table} (${tuple.keys.join(
                ", "
              )}) VALUES (${tuple.values.join(",")});`
            );
          }
          sql.push("end transaction;");
          resolve(sql.join("\n"));
        })
      );
    }
  );
  (await Promise.all(promises)).map((sql) => writer.write(sql));
  await Bun.$`cd tmp && sqlite3 cratesio.sqlite < cratesio.sql && rm cratesio.sql && rm -rf cratesiodump`;
  console.log("  📤 Imports complete");
  db.close();
};

export default generateDB;
