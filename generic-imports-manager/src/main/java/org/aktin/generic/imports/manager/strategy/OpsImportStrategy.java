package org.aktin.generic.imports.manager.strategy;

public class OpsImportStrategy extends AbstractDbImportStrategy {

  public OpsImportStrategy() {
    super("OPS");
  }

  @Override
  protected String buildCondition() {
    return "of.provider_id = 'P21' AND of.concept_cd LIKE 'OPS%'";
  }
}
