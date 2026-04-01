package com.distributed_systems.module_2_data_scaling_service_a.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@Profile({"pool", "docker"})
public class PoolExhaustionConfig {

  @Value("${spring.datasource.url}")
  private String primaryUrl;
  @Value("${spring.datasource.username}")
  private String primaryUser;
  @Value("${spring.datasource.password}")
  private String primaryPass;

  @Bean @Primary
  public DataSource primaryDataSource(){
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(primaryUrl);
    config.setUsername(primaryUser);
    config.setPassword(primaryPass);
    config.setMaximumPoolSize(5);
    config.setConnectionTimeout(3000);
    return new HikariDataSource(config);
  }

  @Bean
  public DataSource reportDataSource(){
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(primaryUrl);
    config.setUsername(primaryUser);
    config.setPassword(primaryPass);
    config.setMaximumPoolSize(3);
    config.setConnectionTimeout(5500);
    return new HikariDataSource(config);
  }

  @Bean @Primary
  public JdbcTemplate primaryDataSourceTemplate(@Qualifier("primaryDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  public JdbcTemplate reportDataSourceTemplate(@Qualifier("reportDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }
}
