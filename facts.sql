select 
enc_map.encounter_num,
pat_map.patient_num, 
fact.concept_cd, 
fact.nval_num, 
fact.modifier_cd, 
fact.start_date 
from "i2b2demodata"."encounter_mapping" enc_map, "i2b2demodata"."patient_mapping" pat_map, "i2b2demodata"."observation_fact" fact 
where pat_map.patient_ide = enc_map.patient_ide and 
pat_map.patient_ide_source = enc_map.patient_ide_source and
pat_map.project_id = enc_map.project_id and 
pat_map.patient_num=fact.patient_num and
enc_map.encounter_num=fact.encounter_num;
