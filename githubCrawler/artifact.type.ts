export namespace Artifact {
  export interface Root {
    [packageName: string]: Package;
  }

  export interface Package {
    coordinates: string;
    name: string;
    description?: string;
    license?: string[];
    categories?: string[];
    tags?: string[];
    ranking?: string;
    users?: number;
    downloads?: number;
    repositories?: string[];
    versions?: Versions;
  }

  export interface Versions {
    [version: string]: Version;
  }
  export interface Version {
    usages: string;
    date: string;
    users: number;
    downloads?: number;
  }
}
