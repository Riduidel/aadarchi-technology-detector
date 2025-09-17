CREATE EXTENSION IF NOT EXISTS tablefunc;

select * from crosstab('
select indicator_date , technology_id, indicator_value from indicator
where technology_id in (2, 17, 2410)
and indicator_name like ''github_forks''
order by indicator_date, indicator_value, technology_id;'
) as ct (indicator_date timestamp(6), react text, vue text, angular text)
;
