package org.aktin.generic.imports.manager.strategy;

public class IcdImportStrategy extends AbstractDbImportStrategy{

  public IcdImportStrategy() {
    super("ICD");
  }

  @Override
  protected String buildCondition() {
    return "of.provider_id = 'P21' AND of.concept_cd LIKE 'ICD10GM%'";
  }
}
