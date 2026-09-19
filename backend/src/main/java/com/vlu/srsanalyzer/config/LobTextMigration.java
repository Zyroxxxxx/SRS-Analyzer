package com.vlu.srsanalyzer.config;

import jakarta.persistence.EntityManager; import lombok.RequiredArgsConstructor; import org.springframework.boot.CommandLineRunner; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional;

@Component @RequiredArgsConstructor
public class LobTextMigration implements CommandLineRunner {
 private final EntityManager em;
 @Override @Transactional public void run(String... args){
  String[] cols={"requirements:raw_description","users:ai_preference","ai_usage:prompt","analysis_results:summary","analysis_results:functional_requirements","analysis_results:non_functional_requirements","analysis_results:ambiguous_notes","analysis_history:summary","analysis_history:functional_requirements","analysis_history:non_functional_requirements","analysis_history:user_stories","analysis_history:acceptance_criteria","analysis_history:ambiguous_notes","user_stories:content","acceptance_criteria:content","audit_logs:details"};
  for(String item:cols){String[] p=item.split(":",2);String table=p[0],col=p[1]; try{em.createNativeQuery("DO $$ BEGIN IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='"+table+"' AND column_name='"+col+"' AND udt_name='oid') THEN EXECUTE 'ALTER TABLE \""+table+"\" ALTER COLUMN \""+col+"\" TYPE TEXT USING CASE WHEN \""+col+"\" IS NULL THEN NULL ELSE convert_from(lo_get(\""+col+"\"), ''UTF8'') END'; END IF; END $$;").executeUpdate();}catch(Exception ignored){}}
 }
}
