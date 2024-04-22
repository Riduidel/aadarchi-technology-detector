import { Database } from "bun:sqlite";
import { Artifact } from "../artifact.type";

const fetchTop1000 = async (): Promise<Artifact.Root> => {
  const db = new Database("tmp/cratesio.sqlite", { readonly: true });
  return {};
};

export default fetchTop1000;
