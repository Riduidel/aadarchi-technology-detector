export type SBOM = {
  SPDXID: string;
  name: string;
  versionInfo: string;
  downloadLocation: string;
  filesAnalyzed: boolean;
  supplier: string;
  externalRefs: [
    {
      referenceCategory: string;
      referenceLocator: string;
      referenceType: string;
    }
  ];
};

export type SBOMLanguages = {
  npm: string[];
  php: string[];
  rust: string[];
};

export type SBOMLibs = {
  npm: string[];
  php: string[];
  rust: string[];
};
