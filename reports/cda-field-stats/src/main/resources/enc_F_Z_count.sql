select distinct f.encounter_num, f.concept_cd, (select modifier_cd from "i2b2crcdata"."observation_fact" subq where subq.concept_cd=f.concept_cd and subq.encounter_num=f.encounter_num and subq.patient_num=f.patient_num and modifier_cd = 'AKTIN:DIAG:F') as fuehrend , (select modifier_cd from "i2b2crcdata"."observation_fact" subq where subq.concept_cd=f.concept_cd and subq.encounter_num=f.encounter_num and subq.patient_num=f.patient_num and modifier_cd in ('AKTIN:DIAG:V','AKTIN:DIAG:Z','AKTIN:DIAG:A','AKTIN:DIAG:G')) as zusatz 
into Temporary TempTable
from "i2b2crcdata"."observation_fact" f 
where f.concept_cd like 'ICD10GM:%' and f.modifier_cd in ('@','AKTIN:DIAG:V','AKTIN:DIAG:Z','AKTIN:DIAG:A','AKTIN:DIAG:G','AKTIN:DIAG:F') 
order by f.encounter_num asc

select encounter_num, fuehrend, zusatz, count(concept_cd) from TempTable
group by encounter_num, fuehrend, zusatz
order by encounter_num, fuehrend, zusatz asc

drop table TempTable