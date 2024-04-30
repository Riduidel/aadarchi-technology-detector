export namespace Registry {
  export interface queryResult {
    id?: string;
    name?: string;
    description?: string;
    dl?: number;
    keyword?: string;
    keywords?: (string | undefined)[];
  }
}
