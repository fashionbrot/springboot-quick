package com.github.fashionbrot.service;

import com.github.fashionbrot.config.MapConfig;
import com.github.fashionbrot.entity.ColumnEntity;
import com.github.fashionbrot.entity.TableEntity;
import com.github.fashionbrot.exception.QuickException;
import com.github.fashionbrot.mapper.BaseMapper;
import com.github.fashionbrot.req.CodeReq;
import com.github.fashionbrot.req.PageReq;
import com.github.fashionbrot.tool.DateUtil;
import com.github.fashionbrot.tool.StringUtil;
import com.github.fashionbrot.vo.PageVo;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class QuickService {

    @Autowired
    private BaseMapper baseMapper;

    @Autowired
    private Environment environment;



    @Autowired
    private MapConfig mapConfig;

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

        String[] tableNames=req.getGenerateTableNames().split(",");
        for(String tableName : tableNames){
            generator(req,tableName);
        }


        return null;
    }

    private void generator(CodeReq req,String tableName){
        TableEntity tableEntity = queryTable(tableName);
        if (tableEntity==null){
            throw new QuickException(tableName+"表不存在，请刷新重试");
        }

        String className = tableToJava(tableEntity.getTableName(), req.getExcludePrefix());
        tableEntity.setClassName(className);
        tableEntity.setVariableClassName(StringUtils.uncapitalize(className));

        boolean hasBigDecimal = false;

        List<ColumnEntity> columns = queryColumns(tableName);
        setDataType(tableEntity, hasBigDecimal, columns);
        tableEntity.setColumns(columns);

        //没主键，则第一个字段为主键
        if(tableEntity.getPrimaryKeyColumnEntity() == null){
            tableEntity.setPrimaryKeyColumnEntity(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);



        //封装模板数据
        VelocityContext velocityContext = initTemplate(req, tableEntity, className, hasBigDecimal);



        try {
            generateTemplate(tableEntity, velocityContext, mapConfig.getVm(),req);

            /*if(flag.isFlag()==false) {
                generateTemplate( zip, velocityContext, fixedVm);
                flag.setFlag(true);
            }*/
        } catch (IOException e) {

        }


    }


    private void generateTemplate(TableEntity tableEntity, VelocityContext context, Map<String,String > vmMap, CodeReq req) throws IOException {
        for(Map.Entry<String,String> map: vmMap.entrySet()){
            String path  = (String) context.get(map.getKey());
            String vm = map.getValue();
            if (StringUtil.isEmpty(path)){
                log.info("vm:"+vm+" 未配置地址");
                continue;
            }

            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(vm, "UTF-8");
            tpl.merge(context, sw);
            //添加到zip
            //替换表前缀
            String className = (String) context.internalGet("className");
            String fileName = path +File.separator+ className+ vm.replaceAll(".vm","").replaceAll("vm/","");

            createFile(fileName,sw.toString());

            System.out.println(fileName);
            /*String fileName =getFileName(template, className, req);
            if (StringUtils.isNotBlank(fileName)) {
                ZipEntry zipEntry = new ZipEntry(fileName);
                zip.putNextEntry(zipEntry);
                IOUtils.write(sw.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            }*/
        }
    }

/*    *//**
     * 获取文件名
     *//*
    public  String getFileName(String template, String className,CodeReq req) {
        String packagePath =getPackagePath(req);


        if (template.contains("Entity.java.vm" )) {
            return packagePath + "entity" + File.separator + className + "Entity.java";
        }
        if (template.contains("Req.java.vm" )) {
            return packagePath + "req" + File.separator + className + "Req.java";
        }

        if (template.contains("Mapper.java.vm" )) {
            return packagePath + "mapper" + File.separator + className + "Mapper.java";
        }


        if (template.contains("Service.java.vm" ) ) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }
        if (template.contains("ServiceImpl.java.vm" )) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }

        if (template.contains("Controller.java.vm" )) {
            return packagePath + "controller" + File.separator + className + "Controller.java";
        }
        if ("on".equals(req.getDtoStatus())){
            if (template.contains("DTO.java.vm" )) {
                return packagePath + File.separator + "dto" + File.separator + className + "DTO.java";
            }
        }

        if (template.contains("Mapper.xml.vm" )) {
            return packagePath + File.separator + "mapper" + File.separator + "xml" + File.separator + className + "Mapper.xml";
        }

        if (template.contains("Mapper.xml.vm" )) {
            return packagePath + File.separator + "mapper" + File.separator + "xml" + File.separator + className + "Mapper.xml";
        }

        return null;
    }*/

    public void createFile(String fileName,String fileContent) {
        try {
            File file=new File(fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if(!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream out=new FileOutputStream(file,true);
            out.write(fileContent.getBytes("utf-8"));
            out.close();
        }catch (Exception e){
            log.error("createFile error",e);
        }

    }

    //封装模板数据
    private VelocityContext initTemplate(CodeReq req, TableEntity tableEntity, String className, boolean hasBigDecimal) {

        Map<String, Object> map = new HashMap<>();

        map.put("out",req.getOut());
        if (StringUtil.isNotEmpty(req.getEntityOut())){
            req.setEntityOut(req.getEntityOut().replaceAll("//","/"));
            if (req.getEntityOut().contains("/")){
                map.put("reqOut",req.getEntityOut().substring(0,req.getEntityOut().lastIndexOf("/"))+File.separator+"req"+File.separator);
            }else if (req.getEntityOut().contains("\\")){
                map.put("reqOut",req.getEntityOut().substring(0,req.getEntityOut().lastIndexOf("\\"))+File.separator+"req"+File.separator);
            }
        }


        map.put("controllerOut",req.getControllerOut());
        map.put("serviceOut",req.getServiceOut());
        if (StringUtil.isNotEmpty(req.getServiceOut())){
            map.put("serviceImplOut",req.getServiceOut()+File.separator+"impl"+File.separator);
        }
        map.put("entityOut",req.getEntityOut());
        map.put("mapperOut",req.getMapperOut());
        map.put("mapperXmlOut",req.getMapperXmlOut());

        map.put("excludePrefix",req.getExcludePrefix());
        map.put("author",req.getAuthor());
        map.put("email",req.getEmail());
        map.put("swaggerStatus","on".equals(req.getSwaggerStatus()));
        map.put("dtoStatus","on".equals(req.getDtoStatus()));



        map.put("oldTableName", tableEntity.getTableName());
        // 处理注释
        if(StringUtils.isNotBlank(tableEntity.getComments())){
            map.put("comments", tableEntity.getComments());
            map.put("commentsEntity", tableEntity.getComments());
            map.put("commentsService", tableEntity.getComments());
            map.put("commentsController", tableEntity.getComments());
            map.put("commentsApi", tableEntity.getComments());
            map.put("commentsDao", tableEntity.getComments());
        }
        map.put("pk", tableEntity.getPrimaryKeyColumnEntity());
        map.put("className", tableEntity.getClassName().replace(captureName(req.getExcludePrefix()),""));

        map.put("variableClassName", tableEntity.getVariableClassName());
        map.put("columns", tableEntity.getColumns());
        StringBuilder sb=new StringBuilder();
        StringBuilder sb2=new StringBuilder();
        for(ColumnEntity c:tableEntity.getColumns()){
            if (StringUtils.isNotBlank(sb.toString())) {
                sb.append(",").append(c.getColumnName());
                sb2.append(",a.").append(c.getColumnName());
            }else{
                sb.append(c.getColumnName());
                sb2.append("a."+c.getColumnName());
            }
        }
        map.put("allColumnNames",sb.toString());
        map.put("allColumnNames2",sb2.toString());

        map.put("hasBigDecimal", hasBigDecimal);
        /* map.put("main", main);*/
        // API接口排序
        if(tableEntity.getCreateTime()!= null){
            map.put("apiSort", StringUtils.substring(String.valueOf(tableEntity.getCreateTime().getTime() + System.currentTimeMillis()), 1, 9));
        }


        if(StringUtils.isNotBlank(tableEntity.getTableName())){
            String tableName = StringUtils.lowerCase(tableEntity.getTableName());
            map.put("pathName", tableName.replace("_","/"));
            map.put("permissionPrefix", tableName.replace("_",":"));

            // 前端权限标识
            map.put("apiPermission", tableName.replace("_",":"));
            // className.toLowerCase()
            map.put("vueFileName", className.toLowerCase());
        }
        SimpleDateFormat sf=new SimpleDateFormat(DateUtil.DATE_FORMAT_SECOND);

        map.put("datetime", DateUtil.formatDate(new Date()));
        map.put("date", DateUtil.formatDate(DateUtil.DATE_FORMAT_DAY_FORMATTER,new Date()));
        map.put("projectName","example");
//        map.put("package2",map.get("package").toString().replace(".","/"));
        VelocityContext context = new VelocityContext(map);

        return context;
    }


    private void setDataType(TableEntity tableEntity, boolean hasBigDecimal, List<ColumnEntity> columns) {
        for(ColumnEntity columnEntity: columns){
            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setVariableAttrName(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            if("NUMBER".equals(columnEntity.getDataType()) && columnEntity.getDataScale() > 0){
                columnEntity.setAttrType("Double");
            }else if("NUMBER".equals(columnEntity.getDataType()) && columnEntity.getDataPrecision()>14){
                columnEntity.setAttrType("Long");
            }else{
                String attrType = environment.getProperty(columnEntity.getDataType(), "unknowType");
                columnEntity.setAttrType(attrType);
                if (!hasBigDecimal && attrType.equals("BigDecimal" )) {
                    hasBigDecimal = true;
                }
            }
            //是否主键
            if("PRI".equalsIgnoreCase(columnEntity.getColumnKey()) && tableEntity.getPrimaryKeyColumnEntity() == null){
                tableEntity.setPrimaryKeyColumnEntity(columnEntity);
            }
        }
    }


    /**
     * 将字符串的首字母转大写
     * @param str 需要转换的字符串
     * @return
     */
    private static String captureName(String str) {
        if (StringUtils.isNotBlank(str)) {
            // 进行字母的ascii编码前移，效率要高于截取字符串进行转换的操作
            char[] cs = str.toCharArray();
            cs[0] -= 32;
            return String.valueOf(cs);
        }
        return "";
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if(StringUtils.isNotBlank(tablePrefix)){
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }
}
