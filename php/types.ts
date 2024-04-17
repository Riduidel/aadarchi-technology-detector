export namespace Registry {
  export interface Popular {
    name: string;
    description: string;
    url: string;
    downloads: number;
    favers: number;
  }

  export interface Package {
    name: string;
    description: string;
    keywords?: string[];
    homepage: string;
    version?: string;
    version_normalized?: string;
    license?: string[];
    authors?: Author[];
    source?: Source;
    dist?: Source;
    type?: string;
    support?: Support;
    funding?: Source[];
    time?: string;
    autoload?: Autoload;
    extra?: Extra;
    require?: Require;
    suggest?: Generic;
    provide?: Generic;
  }

  export interface Author {
    name: string;
    email?: string;
    homepage?: string;
  }

  export interface Source {
    url: string;
    type: string;
    reference: string;
    shasum?: string;
  }

  export interface Support {
    source: string;
  }

  export interface Autoload {
    files?: string[];
    "psr-4"?: Generic;
  }

  export interface Generic {
    [namespace: string]: string;
  }

  export interface Extra {
    thanks: Thanks;
  }

  export interface Thanks {
    name: string;
    url: string;
  }

  export interface Require {
    php?: string;
  }
}

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
