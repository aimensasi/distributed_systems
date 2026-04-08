package com.distributed_systems.module_2_data_scaling_service_a.Configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import javax.xml.crypto.Data;

@Configuration
@Profile("sharding")
public class ShardingConfig {

  @Value("${datasource.shard0.url}")
  private String shard0Url;

  @Value("${datasource.shard1.url}")
  private String shard1Url;

  @Value("${datasource.shard2.url}")
  private String shard2Url;

  @Value("${datasource.shard3.url}")
  private String shard3Url;

  @Value("${spring.datasource.username}")
  private String username;

  @Value("${spring.datasource.password}")
  private String password;

  @Bean
  public DataSource shard0DataSource(){
    return buildDataSource(shard0Url);
  }

  @Bean
  public DataSource shard1DataSource(){
    return buildDataSource(shard1Url);
  }

  @Bean
  public DataSource shard2DataSource(){
    return buildDataSource(shard2Url);
  }

  @Bean
  public DataSource shard3DataSource(){
    return buildDataSource(shard3Url);
  }

  private DataSource buildDataSource(String shardUrl) {
    return DataSourceBuilder.create()
      .url(shardUrl)
      .username(username)
      .password(password)
      .build();
  }

  @Bean
  public JdbcTemplate shard0Template(@Qualifier("shard0DataSource") DataSource dataSource){
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public JdbcTemplate shard1Template(@Qualifier("shard1DataSource") DataSource dataSource){
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public JdbcTemplate shard2Template(@Qualifier("shard2DataSource") DataSource dataSource){
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public JdbcTemplate shard3Template(@Qualifier("shard3DataSource") DataSource dataSource){
    return new JdbcTemplate(dataSource);
  }
}
