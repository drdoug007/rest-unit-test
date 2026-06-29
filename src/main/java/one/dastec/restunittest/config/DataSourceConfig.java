package one.dastec.restunittest.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
    basePackages = "one.dastec.restunittest.repositories",
    entityManagerFactoryRef = "historyEntityManagerFactory",
    transactionManagerRef = "historyTransactionManager"
)
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.configuration")
    public DataSource primaryDataSource() {
        return primaryDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("app.history-datasource")
    public DataSourceProperties historyDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "historyDataSource")
    @ConfigurationProperties("app.history-datasource.configuration")
    public DataSource historyDataSource() {
        return historyDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "historyEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean historyEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("historyDataSource") DataSource dataSource) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        
        return builder
                .dataSource(dataSource)
                .packages("one.dastec.restunittest.entities")
                .persistenceUnit("history")
                .properties(properties)
                .build();
    }

    @Bean(name = "historyTransactionManager")
    public PlatformTransactionManager historyTransactionManager(
            @Qualifier("historyEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
