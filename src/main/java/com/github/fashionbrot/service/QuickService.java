package com.github.fashionbrot.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.github.fashionbrot.entity.ColumnEntity;
import com.github.fashionbrot.entity.TableEntity;
import com.github.fashionbrot.mapper.BaseMapper;
import com.github.fashionbrot.req.CodeReq;
import com.github.fashionbrot.req.PageReq;
import com.github.fashionbrot.vo.PageVo;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuickService {

    @Autowired
    private BaseMapper baseMapper;

    public PageVo queryList(PageReq req) {
        Page<?> page = PageHelper.startPage(req.getPage(),req.getPageSize());
        Map<String,Object> map=new HashMap();

        map.put("tableName",req.getTableName());
        List<TableEntity> list = baseMapper.tableList(map);

        return PageVo.builder()
                .rows(list)
                .total(page.getTotal())
                .build();
    }


    public TableEntity queryTable(String tableName) {
        return baseMapper.queryTable(tableName);
    }

    public List<ColumnEntity> queryColumns(String tableName) {
        return baseMapper.queryColumns(tableName);
    }

    public byte[] generatorCode(CodeReq req) {
        /*ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        Flag flag=new Flag();
        String[] tableNames=req.getTables().split(",");
        for(String tableName : tableNames){
            scaffoldUtil.generator( req,queryTable(tableName), queryColumns(tableName), zip,flag);
        }

        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();*/
        return null;
    }

    public static void main(String[] args) {
        for(int i=0;i<1000;i++){
            System.out.println(NanoIdUtils.randomNanoId());
        }
    }
}
