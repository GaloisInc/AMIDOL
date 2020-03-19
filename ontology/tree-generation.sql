SELECT id FROM snomed WHERE parents LIKE (SELECT id FROM snomed WHERE terms LIKE '%H3N2%')
