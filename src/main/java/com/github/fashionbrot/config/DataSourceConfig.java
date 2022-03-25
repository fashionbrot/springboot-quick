package com.github.fashionbrot.config;


import com.alibaba.druid.pool.DruidDataSource;
import com.github.fashionbrot.exception.QuickException;
import com.github.fashionbrot.mapper.BaseMapper;
import com.github.fashionbrot.mapper.MysqlMapper;
import com.github.fashionbrot.mapper.OracleMapper;
import com.github.fashionbrot.mapper.SqlServerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;


@SuppressWarnings("ALL")
@Configuration
@Component
public class DataSourceConfig {

    @Autowired
    private MysqlMapper mysqlMapper;
    @Autowired
    private OracleMapper oracleMapper;
    @Autowired
    private SqlServerMapper sqlServerMapper;


    @Autowired
    private DruidDataSource db;

    @Bean
    @Primary
    public BaseMapper getGeneratorDao(){

        if(db.getDriverClassName().contains("mysql")){
            return mysqlMapper;
        }else if(db.getDriverClassName().contains("oracle")){
            return oracleMapper;
        }else if(db.getDriverClassName().contains("sqlserver")){
            return sqlServerMapper;
        }else {
            throw new QuickException("不支持当前数据库：" + db.getDriverClassName());
        }
    }

}
