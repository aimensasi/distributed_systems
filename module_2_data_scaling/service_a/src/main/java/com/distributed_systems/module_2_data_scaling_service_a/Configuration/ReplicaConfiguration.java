package com.distributed_systems.module_2_data_scaling_service_a.Configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class ReplicaConfiguration {

  @Value("${spring.datasource.url}")
  private String primaryUrl;
  @Value("${spring.datasource.username}")
  private String primaryUser;
  @Value("${spring.datasource.password}")
  private String primaryPass;

  @Value("${replica.datasource.url}")
  private String replicaUrl;
  @Value("${replica.datasource.username}")
  private String replicaUser;
  @Value("${replica.datasource.password}")
  private String replicaPass;

  @Primary
  @Bean
  public DataSource primaryConnection(){
    return DataSourceBuilder.create()
      .url(primaryUrl)
      .username(primaryUser)
      .password(primaryPass)
      .build();
  }

  @Bean
  public DataSource replicaConnection(){
    return DataSourceBuilder.create()
      .url(replicaUrl)
      .username(replicaUser)
      .password(replicaPass)
      .build();
  }

  @Bean @Primary
  public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryConnection") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  public JdbcTemplate replicaJdbcTemplate(@Qualifier("replicaConnection") DataSource ds) {
    return new JdbcTemplate(ds);
  }
}
