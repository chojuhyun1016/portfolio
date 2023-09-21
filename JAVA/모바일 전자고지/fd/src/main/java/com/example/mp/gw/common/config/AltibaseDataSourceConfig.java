package com.example.mp.gw.common.config;


import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.sun.el.parser.ParseException;


@Configuration
@MapperScan(value = "com.uplus.mp.gw.**.mappers.altibase", sqlSessionFactoryRef = "altibaseSqlSessionFactory")
@EnableTransactionManagement
public class AltibaseDataSourceConfig
{
	@Bean (name = "altibaseDatasource", destroyMethod = "close")
	@ConfigurationProperties (prefix = "spring.altibase.datasource")
	public DataSource altibaseDataSource()
	{
		return DataSourceBuilder.create().build();
	}

    @Bean (name="altibaseSqlSessionFactory")
	public SqlSessionFactory altibaseSqlSessionFactory(	@Qualifier("altibaseDatasource") DataSource firstDataSource
													  , ApplicationContext applicationContext
													  ) throws Exception
    {		
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean(); 
		
		sqlSessionFactoryBean.setDataSource(firstDataSource); 
		sqlSessionFactoryBean.setMapperLocations(applicationContext.getResources("classpath:mappers/altibase/**/*.xml"));
		sqlSessionFactoryBean.setTypeAliasesPackage("com.uplus.mp.gw.**.domain");
		sqlSessionFactoryBean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
		sqlSessionFactoryBean.getObject().getConfiguration().setCacheEnabled(false);
		sqlSessionFactoryBean.getObject().getConfiguration().setJdbcTypeForNull(null);
		
		return sqlSessionFactoryBean.getObject(); 
	}

	@Bean (name= "altibaseSqlSessionTemplate")
	public SqlSessionTemplate altibaseSqlSessionTemplate(@Qualifier("altibaseSqlSessionFactory") SqlSessionFactory altibaseSqlSessionFactory) throws Exception
	{
		return new SqlSessionTemplate(altibaseSqlSessionFactory);
	}

	@Bean
	public PlatformTransactionManager transactionManager() throws URISyntaxException, GeneralSecurityException, ParseException, RuntimeException, IOException
	{
		return new DataSourceTransactionManager(altibaseDataSource());
	}
}
