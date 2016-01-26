select 
enc_map.encounter_num, 
pat.patient_num, 
pat.birth_date, 
pat.sex_cd, 
( select start_date from "i2b2demodata"."observation_fact" subq where subq.concept_cd='LOINC:52455-3' and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as ZeitpunktAufnahme, 
( select concept_cd from "i2b2demodata"."observation_fact" subq where subq.concept_cd like 'AKTIN:ISOLATION:%' and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as Isolation, 
( select concept_cd from "i2b2demodata"."observation_fact" subq where subq.concept_cd like 'AKTIN:ISOREASON:%' and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as IsolationReason, 
( select concept_cd from "i2b2demodata"."observation_fact" subq where subq.concept_cd like 'AKTIN:REFERRAL:%' and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as Zuweisung, 
( select start_date from "i2b2demodata"."observation_fact" subq where (subq.concept_cd like 'MTS:%' or subq.concept_cd like 'ESI:%') and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as ZeitpunktTriage, 
( select concept_cd from "i2b2demodata"."observation_fact" subq where (subq.concept_cd like 'MTS:%' or subq.concept_cd like 'ESI:%') and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as Triage, 
( select start_date from "i2b2demodata"."observation_fact" subq where subq.concept_cd = 'AKTIN:ZeitpunktErsterArztkontakt' and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as ZeitpunktArztkontakt, 
( select start_date from "i2b2demodata"."observation_fact" subq where subq.concept_cd = 'AKTIN:ZeitpunktTherapiebeginn' and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as ZeitpunktTherapie,  
( select concept_cd from "i2b2demodata"."observation_fact" subq where subq.concept_cd like 'CEDIS30:%' and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as CEDIS, 
( select concept_cd from "i2b2demodata"."observation_fact" subq where subq.concept_cd like 'AKTIN:PATHOGENE:%' and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as Keime, 
( select start_date from "i2b2demodata"."observation_fact" subq where subq.concept_cd='AKTIN:ZeitpunktErsterArztkontakt' and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as ZeitpunktArzt, 
( select concept_cd from "i2b2demodata"."observation_fact" subq where subq.concept_cd like 'AKTIN:TRANSPORT:%' and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as Transportmittel, 
( select concept_cd from "i2b2demodata"."observation_fact" subq where (subq.concept_cd like 'AKTIN:DISCHARGE:%' or subq.concept_cd like 'AKTIN:TRANSFER:%') and subq.modifier_cd = '@' and pat.patient_num=subq.patient_num and enc_map.encounter_num=subq.encounter_num ) as Entlassung 
from "i2b2demodata"."encounter_mapping" enc_map, "i2b2demodata"."patient_mapping" pat_map, "i2b2demodata"."patient_dimension" pat 
where 
pat_map.patient_ide = enc_map.patient_ide and 
pat_map.patient_ide_source = enc_map.patient_ide_source and 
pat_map.project_id = enc_map.project_id and 
pat.patient_num = pat_map.patient_num and
pat.patient_num=pat.patient_num ;