export namespace Registry {
  export interface Popular {
    objects: PopularObject[];
  }

  export interface PopularObject {
    package: Package;
    flags: any;
    score: any;
    searchScore: number;
  }

  export interface Package {
    _id: string;
    _rev: string;
    /** the name of the package */
    name: string;
    /** the description of the package */
    description: string;
    "dist-tags": DistTags;
    /** the versions of the package */
    versions: Versions;
    readme: string;
    maintainers?: MaintainersEntityOrAuthorOrNpmUser[] | null;
    time: Time;
    author: MaintainersEntityOrAuthorOrNpmUser;
    /** the license of the package */
    license: string;
    readmeFilename: string;
    /** the keywords/tags of the package */
    keywords: string[];
    /** the users of the package */
    users: {
      [userName: string]: boolean;
    };
    repository: Repository;
  }

  export interface Repository {
    type: string;
    url: string;
  }

  export interface DistTags {
    latest: string;
  }
  export interface Versions {
    [version: string]: Version;
  }
  export interface Version {
    name: string;
    version: string;
    description: string;
    main: string;
    scripts: Scripts;
    author: MaintainersEntityOrAuthorOrNpmUser;
    license: string;
    _id: string;
    _shasum: string;
    _from: string;
    _npmVersion: string;
    _nodeVersion: string;
    _npmUser: MaintainersEntityOrAuthorOrNpmUser;
    maintainers?: MaintainersEntityOrAuthorOrNpmUser[] | null;
    dist: Dist;
  }
  export interface Scripts {
    test: string;
  }
  export interface MaintainersEntityOrAuthorOrNpmUser {
    name: string;
    email: string;
  }
  export interface Dist {
    shasum: string;
    tarball: string;
    integrity: string;
    signatures?: SignaturesEntity[] | null;
  }
  export interface SignaturesEntity {
    keyid: string;
    sig: string;
  }
  export interface Time {
    modified: string;
    created: string;
    [version: string]: string;
  }

  export interface BulkDownloadsCounter {
    [package: string]: DownloadsCounter;
  }
  export interface DownloadsCounter {
    downloads: number;
    start: string;
    end: string;
    package: string;
  }
  export interface DownloadsCounterPerVersion {
    package: string;
    /** Counters of download per version  */
    downloads: {
      [version: string]: number;
    };
  }
}

export interface DownloadCounter {
  [packageName: string]: number;
}

export interface GetBulkPackagesOptions {
  withDownloads?: boolean;
  withScopedDownloads?: boolean;
}
