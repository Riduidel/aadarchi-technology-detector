delete from "indicator" i where i.indicator_date > '2025-07-01'::date;
delete from "github_stars" g where g.star_date > '2025-07-01'::date;
delete from "github_forks" g where g.fork_date > '2025-07-01'::date;