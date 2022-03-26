package com.github.fashionbrot.jdbc;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.github.fashionbrot.exception.QuickException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;

@Service
@Slf4j
public class JdbcService<T> {


    public static Connection connectJdbc(JdbcReq req) {

        try {
            Class.forName(req.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new QuickException("未找到 驱动 ：" + req.getDriverClassName());
        }

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(req.getUrl(), req.getUsername(), req.getPassword());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            QuickException.throwMsg("连接数据库失败，请检查：" + throwables.getMessage());
        }
        if (connection == null) {
            QuickException.throwMsg("连接数据库失败");
        }
        return connection;
    }

    public static ResultSet querySql(Connection connection, String sql,  Object... params) {

        if (connection != null) {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = connection.prepareStatement(sql);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                QuickException.throwMsg("执行sql 失败：" + throwables.getMessage());
            }
            if (preparedStatement != null) {
                if (params != null && params.length > 0) {
                    int index = 1;
                    for (Object p : params) {
                        if (p == null) {
                            continue;
                        }
                        try {
                            if (p instanceof String) {
                                String param = (String) p;
                                if (StringUtils.isNotEmpty(param)) {
                                    preparedStatement.setString(index, param);
                                    index++;
                                }
                            }
                            if (p instanceof Integer) {
                                preparedStatement.setInt(index, (Integer) p);
                                index++;
                            }
                        } catch (SQLException e) {
                            QuickException.throwMsg("执行sql 参数失败" + e.getMessage());
                        }

                    }
                }

                try {
                    return preparedStatement.executeQuery();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }

        return null;
    }

    public static void closeConnect(Connection connection){
        if (connection!=null){
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

}
