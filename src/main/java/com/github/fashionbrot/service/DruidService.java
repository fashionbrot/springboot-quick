package com.github.fashionbrot.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;
import com.github.fashionbrot.Consts.GlobalConsts;
import com.github.fashionbrot.exception.QuickException;
import com.github.fashionbrot.req.DatabaseReq;
import com.github.fashionbrot.util.FileUtil;
import com.github.fashionbrot.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DruidService {

    @Autowired
    private Environment environment;


    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private DruidDataSource druidDataSource;


    public String getPath(){

        String path = environment.getProperty("mars.quick.cache.path");
        if (StringUtils.isEmpty(path)) {
            path = FileUtil.USER_HOME;
        }
        path = path + File.separator + GlobalConsts.NAME+File.separator;
        return path;
    }


    public String getFileName(){
        String marsQuickCacheName = environment.getProperty("mars.quick.cache.name");
        return marsQuickCacheName;
    }


    public List<DatabaseReq> getDatabaseList(){
        String path = getPath()+getFileName();

        List<File> files = FileUtil.searchFiles(new File(path), getFileName());
        if (!CollectionUtils.isEmpty(files)) {
            String fileContent = FileUtil.getFileContent(files.get(0));
            if (StringUtils.isEmpty(fileContent)) {
                return null;
            }
            return JsonUtil.parseArray(DatabaseReq.class, fileContent);
        }
        return null;
    }



    public void reload(DatabaseReq req){
        if (StringUtils.isEmpty(req.getName())) {
            QuickException.throwMsg("请输入此配置名称");
        }
        if (StringUtils.isEmpty(req.getUrl())) {
            QuickException.throwMsg("请输入数据库配置");
        }
        try {
            druidDataSource.restart();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


        String marsQuickCacheName =getFileName();
        if (StringUtils.isEmpty(marsQuickCacheName)) {
            QuickException.throwMsg("mars.quick.cache.name The value cannot be empty. Please configure it");
        }
        String path =getPath();
        String filePath = path + marsQuickCacheName;

        List<File> files = FileUtil.searchFiles(new File(filePath), marsQuickCacheName);

        List<DatabaseReq> old = new ArrayList<>();
        if (!CollectionUtils.isEmpty(files)) {
            old.add(req);
            String fileContent = FileUtil.getFileContent(files.get(0));
            List<DatabaseReq> databases = JsonUtil.parseArray(DatabaseReq.class, fileContent);
            if (!CollectionUtils.isEmpty(databases)){
                for(int i=0;i<databases.size();i++){
                    DatabaseReq databaseReq = databases.get(i);
                    if (databaseReq!=null && !databaseReq.getName().equals(req.getName())){
                        old.add(databaseReq);
                    }
                }
            }

        }else{
            old.add(req);
        }

        FileUtil.deleteFile(new File(filePath));
        FileUtil.writeFile(new File(filePath), JSON.toJSONString(old));

        druidDataSource.setUsername(req.getUsername());
        druidDataSource.setPassword(req.getPassword());
        druidDataSource.setUrl(req.getUrl());
        druidDataSource.setDriverClassName(req.getDriverClassName());

    }



}
