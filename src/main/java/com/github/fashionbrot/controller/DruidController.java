package com.github.fashionbrot.controller;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.support.BiIntFunction;
import com.github.fashionbrot.Consts.GlobalConsts;
import com.github.fashionbrot.exception.QuickException;
import com.github.fashionbrot.req.DatabaseReq;
import com.github.fashionbrot.service.DruidService;
import com.github.fashionbrot.tool.CollectionUtil;
import com.github.fashionbrot.tool.StringUtil;
import com.github.fashionbrot.util.FileUtil;
import com.github.fashionbrot.util.JsonUtil;
import com.github.fashionbrot.vo.DatabaseVo;
import com.github.fashionbrot.vo.RespVo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class DruidController {

    @Autowired
    private Environment environment;

    @Autowired
    private DruidService druidService;

    @ResponseBody
    @RequestMapping("/reload")
    public RespVo reload(DatabaseReq req) {
        druidService.reload(req);
        return RespVo.success();
    }



    @ResponseBody
    @RequestMapping("/load")
    public RespVo load() {

        List<DatabaseReq> databaseList = druidService.getDatabaseList();

        return RespVo.success(databaseList);
    }


    @ResponseBody
    @RequestMapping("/remove")
    public RespVo remove(DatabaseReq req) {
        String marsQuickCacheName = druidService.getFileName();
        String path = druidService.getPath();

        List<DatabaseReq> databaseList = druidService.getDatabaseList();

        List<DatabaseReq> old = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(databaseList)){
            for (int i = 0; i <databaseList.size() ; i++) {
                DatabaseReq databaseReq = databaseList.get(i);
                if (databaseReq!=null && !databaseReq.getName().equals(req.getName())){
                    old.add(databaseReq);
                }
            }
        }

        String filePath = path + marsQuickCacheName;
        FileUtil.deleteFile(new File(filePath));
        FileUtil.writeFile(new File(filePath), JSON.toJSONString(old));

        return RespVo.success();
    }




}
