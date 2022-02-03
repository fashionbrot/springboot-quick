package com.github.fashionbrot.jdbc;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import com.alibaba.fastjson.JSON;
import com.github.fashionbrot.entity.ColumnEntity;
import com.github.fashionbrot.entity.TableEntity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResultSetMapper{


    public static  <T> List<T> getList(Class<T> clazz, ResultSet rs) {
        Field field = null;
        List<T> lists = new ArrayList<T>();
        try {
            // 取得ResultSet列名
            ResultSetMetaData rsmd = rs.getMetaData();
            // 获取记录集中的列数
            int counts = rsmd.getColumnCount();
            // 定义counts个String 变量
            String[] columnNames = new String[counts];
            // 给每个变量赋值(字段名称全部转换成小写)
            for (int i = 0; i < counts; i++) {
                columnNames[i] = rsmd.getColumnLabel(i + 1);
            }
            // 变量ResultSet
            while (rs.next()) {
                T t = clazz.newInstance();
                // 反射, 从ResultSet绑定到JavaBean
                for (int i = 0; i < counts; i++) {

                    // 设置参数类型，此类型应该跟javaBean 里边的类型一样，而不是取数据库里边的类型
                    field = clazz.getDeclaredField(columnNames[i]);

                    // 这里是获取bean属性的类型
                    Class<?> beanType = field.getType();

                    // 根据 rs 列名 ，组装javaBean里边的其中一个set方法，object 就是数据库第一行第一列的数据了
                    Object value = rs.getObject(columnNames[i]);

                    if (value != null) {

                        // 这里是获取数据库字段的类型
                        Class<?> dbType = value.getClass();

                        if (dbType == java.time.LocalDateTime.class && beanType == java.util.Date.class){
                            java.time.LocalDateTime localDateTime = (LocalDateTime) value;
                            ZoneId zone = ZoneId.systemDefault();
                            Instant instant = localDateTime.atZone(zone).toInstant();
                            java.util.Date date = Date.from(instant);

                            value = date;
                        }else

                        // 处理日期类型不匹配问题
                        if ((dbType == java.sql.Timestamp.class )
                                && beanType == java.util.Date.class) {
                            // value = new
                            // java.util.Date(rs.getTimestamp(columnNames[i]).getTime());
                            value = new java.util.Date(
                                    ((java.sql.Timestamp) value).getTime());
                        }
                        // 处理double类型不匹配问题
                        if (dbType == java.math.BigDecimal.class
                                && beanType == double.class) {
                            // value = rs.getDouble(columnNames[i]);
                            value = new Double(value.toString());
                        }
                        // 处理int类型不匹配问题
                        if (dbType == java.math.BigDecimal.class
                                && beanType == int.class) {
                            // value = rs.getInt(columnNames[i]);
                            value = new Integer(value.toString());
                        }
                    }



                    String setMethodName = "set"
                            + firstUpperCase(columnNames[i]);
                    // 第一个参数是传进去的方法名称，第二个参数是 传进去的类型；
                    Method m = t.getClass().getDeclaredMethod(setMethodName, beanType);

                    // 第二个参数是传给set方法数据；如果是get方法可以不写
                    m.invoke(t, value);
                }
                lists.add(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return lists;
    }

    /**
     * @param clazz
     *            bean类
     * @param rs
     *            结果集 (只有封装第一条结果)
     * @return 封装了查询结果的bean对象
     */
    public static <T> T getObj(Class<T> clazz, ResultSet rs) {
        Field field = null;
        T obj = null;
        try {
            // 取得ResultSet列名
            ResultSetMetaData rsmd = rs.getMetaData();
            // 获取记录集中的列数
            int counts = rsmd.getColumnCount();
            // 定义counts个String 变量
            String[] columnNames = new String[counts];
            // 给每个变量赋值(字段名称全部转换成小写)
            for (int i = 0; i < counts; i++) {
                columnNames[i] = rsmd.getColumnLabel(i + 1);
            }
            // 变量ResultSet
            if (rs.next()) {
                T t = clazz.newInstance();
                // 反射, 从ResultSet绑定到JavaBean
                for (int i = 0; i < counts; i++) {
                    try{
                        // 设置参数类型，此类型应该跟javaBean 里边的类型一样，而不是取数据库里边的类型
                        field = clazz.getDeclaredField(columnNames[i]);
                    }catch(Exception ex){
                        ex.printStackTrace();
                        continue;
                    }

                    // 这里是获取bean属性的类型
                    Class<?> beanType = field.getType();

                    // 根据 rs 列名 ，组装javaBean里边的其中一个set方法，object 就是数据库第一行第一列的数据了
                    Object value = rs.getObject(columnNames[i]);

                    if (value != null) {

                        // 这里是获取数据库字段的类型
                        Class<?> dbType = value.getClass();


                        if (dbType == java.time.LocalDateTime.class && beanType == java.util.Date.class){
                            java.time.LocalDateTime localDateTime = (LocalDateTime) value;
                            ZoneId zone = ZoneId.systemDefault();
                            Instant instant = localDateTime.atZone(zone).toInstant();
                            java.util.Date date = Date.from(instant);

                            value = date;
                        }else
                        // 处理日期类型不匹配问题
                        if ((dbType == java.sql.Timestamp.class )
                                && beanType == java.util.Date.class) {
                            // value = new
                            // java.util.Date(rs.getTimestamp(columnNames[i]).getTime());
                            value = new java.util.Date(
                                    ((java.sql.Timestamp) value).getTime());
                        }
                        // 处理double类型不匹配问题
                        if (dbType == java.math.BigDecimal.class
                                && beanType == double.class) {
                            // value = rs.getDouble(columnNames[i]);
                            value = new Double(value.toString());
                        }
                        // 处理int类型不匹配问题
                        if (dbType == java.math.BigDecimal.class
                                && beanType == int.class) {
                            // value = rs.getInt(columnNames[i]);
                            value = new Integer(value.toString());
                        }
                    }

                    String setMethodName = "set"
                            + firstUpperCase(columnNames[i]);
                    // 第一个参数是传进去的方法名称，第二个参数是 传进去的类型；
                    Method m = t.getClass().getDeclaredMethod(setMethodName, beanType);

                    // 第二个参数是传给set方法数据；如果是get方法可以不写
                    m.invoke(t, value);
                }
                obj = t;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return obj;
    }

    // 首写字母变大写
    public static String firstUpperCase(String old) {
        return old.substring(0, 1).toUpperCase() + old.substring(1);
    }




    public static void main(String[] args) throws SQLException {
        JdbcReq req=JdbcReq.builder()
                .driverClassName("com.mysql.jdbc.Driver")
                .url("jdbc:mysql://127.0.0.1:3306/mdc?Unicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&tinyInt1isBit=false")
                .username("root")
                .password("123456")
                .build();

        Connection connection = null;
        ResultSet resultSet = null;
        try {

            connection = JdbcService.connectJdbc(req);

            String sql = "select table_name tableName, engine, table_comment comments, create_time createTime from information_schema.tables "+
                    " where table_schema = (select database())   order by create_time desc";
            resultSet = JdbcService.querySql(connection, sql, "");

            List<TableEntity> tableEntities =getList (TableEntity.class,resultSet);
            System.out.println(JSON.toJSONString(tableEntities));

            String tableSql = "select column_name columnName, data_type dataType, column_comment comments, column_key columnKey, extra from information_schema.columns\n" +
                    " \t\t\twhere table_name =? and table_schema = (select database()) order by ordinal_position";

            resultSet = JdbcService.querySql(connection, tableSql, "sys_menu");
            List<ColumnEntity> columnEntityList =getList (ColumnEntity.class,resultSet);
            System.out.println(JSON.toJSONString(columnEntityList));

        }catch (Exception e){
            log.error(" error",e);
        }finally {
            if (resultSet!=null) {
                resultSet.close();
            }
            JdbcService.closeConnect(connection);
        }

    }

}
