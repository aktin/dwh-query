select distinct concept_cd, modifier_cd, count (distinct encounter_num) from "i2b2crcdata"."observation_fact" where modifier_cd in ('@','AKTIN:TSITE:R','AKTIN:TSITE:L','AKTIN:RESULT:PB','AKTIN:RESULT:OPB','AKTIN:DIAG:V','AKTIN:DIAG:Z','AKTIN:DIAG:A','AKTIN:DIAG:G','AKTIN:DIAG:F') and concept_cd not in ('AKTIN:SYMPTOMDURATION','LOINC:9279-1','LOINC:20564-1','LOINC:8480-6','LOINC:8867-4','LOINC:8329-5','LOINC:72514-3','LOINC:9269-2','LOINC:9267-6','LOINC:9270-0','LOINC:9268-4','LOINC:75859-9') group by concept_cd, modifier_cd
union
select 'SEX:MALE' as concept_cd, '@' as modifier_cd, count(distinct f.encounter_num) from "i2b2crcdata"."patient_dimension" as p, "i2b2crcdata"."observation_fact" as f where p.patient_num = f.patient_num and p.sex_cd = 'M'
union
select 'SEX:FEMALE' as concept_cd, '@' as modifier_cd, count(distinct f.encounter_num) from "i2b2crcdata"."patient_dimension" as p, "i2b2crcdata"."observation_fact" as f where p.patient_num = f.patient_num and p.sex_cd = 'F'
union
select 'AKTIN:ADMISSIONTIME' as concept_cd, '@' as modifier_cd, count(start_date) from "i2b2crcdata"."visit_dimension"
union
select 'AKTIN:DISCHARGETIME' as concept_cd, '@' as modifier_cd, count(end_date) from "i2b2crcdata"."visit_dimension"
union
select distinct concept_cd, modifier_cd, count (distinct encounter_num) from "i2b2crcdata"."observation_fact" where modifier_cd = 'effectiveTime' and concept_cd in ('LOINC:26436-6','LOINC:18767-4', 'LOINC:50556-0', 'LOINC:34534-8', 'LOINC:25061-3', 'LOINC:42148-7', 'LOINC:24725-4', 'LOINC:25045-6', 'LOINC:46305-9','LOINC:38008-9', 'LOINC:30745-4', 'LOINC:28561-9', 'LOINC:37637-6', 'LOINC:43468-8', 'LOINC:25056-3') group by concept_cd, modifier_cd
union
select 'MTS', modifier_cd, count (distinct encounter_num) from "i2b2crcdata"."observation_fact" where modifier_cd = 'effectiveTimeLow' and concept_cd like 'MTS:%'  group by modifier_cd
union
select 'ESI', modifier_cd, count (distinct encounter_num) from "i2b2crcdata"."observation_fact" where modifier_cd = 'effectiveTimeLow' and concept_cd like 'ESI:%'  group by modifier_cd
union
select concept_cd, modifier_cd, count (distinct encounter_num) from "i2b2crcdata"."observation_fact" where modifier_cd = 'effectiveTimeLow' and concept_cd = 'AKTIN:ASSESSMENT'  group by concept_cd, modifier_cd
order by concept_cd asc