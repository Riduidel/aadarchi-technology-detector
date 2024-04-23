import { Database } from "bun:sqlite";
import parse from "csv-simple-parser";

type genType = {
  [key: string]: string[];
};

const tables: genType = {
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
  version_downloads: [
    "version_id",
    "downloads",
    "counted",
    "date",
    "processed",
  ],
  versions: [
    "id",
    "crate_id",
    "num",
    "updated_at",
    "created_at",
    "downloads",
    "features",
    "yanked",
    "license",
    "crate_size",
    "published_by",
    "checksum",
    "links",
    "rust_version",
    "semver_no_prerelease",
  ],
};

const noImports: genType = {
  crates: [
    "readme",
    "updated_at",
    "created_at",
    "homepage",
    "documentation",
    "textsearchable_index_col",
    "repository",
    "max_upload_size",
    "max_features",
  ],
  keywords: ["crates_cnt", "created_at"],
  version_downloads: ["counted", "processed"],
  versions: ["id", "crate_id", "created_at", "downloads", "license"],
};

const keys: genType = {
  crate_downloads: ["crate_id"],
  crates_keywords: ["crate_id", "keyword_id"],
  keywords: ["id"],
  crates: ["id"],
  version_downloads: ["version_id", "date"],
  versions: ["id", "crate_id"],
};

const schema = async () => {
  const file = Bun.file("tmp/cratesio.schema.sql");
  const writer = file.writer({ highWaterMark: 1024 * 1024 });

  Object.entries(tables).forEach(([table, columns]) => {
    writer.write(`
CREATE TABLE IF NOT EXISTS ${table} (${columns
      .map((col) =>
        col === "id" || col.endsWith("_id")
          ? `${col} INTEGER NOT NULL`
          : `${col} TEXT`
      )
      .join(", ")},
    PRIMARY KEY (${keys[table].join(", ")}));
DELETE FROM ${table};\n`);
  });
  writer.flush();
  await Bun.$`cd tmp && sqlite3 cratesio.sqlite < cratesio.schema.sql && rm cratesio.schema.sql`;
};

const importTable = async (table: string, inserts: string[]) => {
  const sql = Bun.file(`tmp/cratesio.${table}.sql`);
  const writer = sql.writer({ highWaterMark: 1024 * 1024 });
  writer.write("begin transaction;\n");
  writer.write(inserts.join("\n"));
  writer.write("\nend transaction;");
  await Bun.$`cd tmp && sqlite3 cratesio.sqlite < cratesio.${table}.sql && rm cratesio.${table}.sql`;
};

const generateDB = async () => {
  console.log("  🎣 fetch dump file online");
  await Bun.$`wget -O tmp/cratesiodump.tar.gz https://static.crates.io/db-dump.tar.gz`.quiet();
  await Bun.$`cd tmp && rm -rf cratesiodump && mkdir cratesiodump && tar -xzvf cratesiodump.tar.gz -C cratesiodump && rm cratesiodump.tar.gz`.quiet();
  const dir = (await Bun.$`ls -d tmp/cratesiodump/*`.text()).trim();
  await Bun.$`rm cratesio.sqlite`.quiet();
  console.log("  🗄️ Create Database");
  const db = new Database("tmp/cratesio.sqlite", { create: true });

  await schema();

  for await (const table of [...Object.keys(tables)]) {
    console.log(`    📥 Import ${table}: ${dir}/data/${table}.csv`);
    const file = Bun.file(`${dir}/data/${table}.csv`);
    const csv = parse(await file.text(), { header: true }) as object[];
    let data: string[] = [];
    let i = 0;
    let j = 0;
    const length = csv.length;
    for (const row of csv) {
      const tuple: genType = { keys: [], values: [] };
      for (const [key, value] of Object.entries(row)) {
        if (noImports[table]?.includes(key)) {
        } else {
          tuple.keys.push(key);
          if (key === "id" || key.endsWith("_id")) tuple.values.push(value);
          else
            tuple.values.push(
              `"${value
                .replaceAll('"', '""')
                .replaceAll("'", "''")
                .replaceAll("\n", " ")}"`
            );
        }
      }
      const insert = `INSERT INTO ${table} (${tuple.keys.join(
        ", "
      )}) VALUES (${tuple.values.join(",")});`;
      if (insert.indexOf("()") === -1) data.push(insert);
      i++;
      if (i === 1000000) {
        j += i;
        console.log(`    📩 Importing ${j} / ${length}`);
        await importTable(table, data);
        i = 0;
        data = [];
      }
    }
    if (data.length) {
      console.log(`    📩 Importing ${length} / ${length}`);
      await importTable(table, data);
    }
    await Bun.$`rm ${dir}/data/${table}.csv`;
  }
  await Bun.$`cd tmp && rm -rf cratesiodump`;
  console.log("  📤 Imports complete");

  db.close();
};

export default generateDB;
