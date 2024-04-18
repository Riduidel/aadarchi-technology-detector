export type Sites = {
  [key: string]: Site;
};

export type Site = {
  selector: string;
  pages: string[];
};
