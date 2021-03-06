package com.github.fashionbrot.service;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.github.fashionbrot.config.MapConfig;
import com.github.fashionbrot.entity.ColumnEntity;
import com.github.fashionbrot.entity.TableEntity;
import com.github.fashionbrot.exception.QuickException;
import com.github.fashionbrot.mapper.BaseMapper;
import com.github.fashionbrot.req.CodeReq;
import com.github.fashionbrot.req.DatabaseReq;
import com.github.fashionbrot.req.PageReq;
import com.github.fashionbrot.vo.PageVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.ibatis.type.Alias;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
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
    private DruidService druidService;


    @Autowired
    private MapConfig mapConfig;

    private String SRC_MAIN_JAVA = File.separator+"src"+File.separator+"main"+File.separator+"java";
    private String SRC_MAIN_RESOURCES = File.separator+"src"+File.separator+"main"+File.separator+"resources";

    public PageVo queryList(PageReq req) {

        reloadDatabase(req.getDatabaseName());



        Map<String,Object> map=new HashMap();

        map.put("tableName",req.getTableName());
        List<TableEntity> list = baseMapper.tableList(map);

        return PageVo.builder()
                .rows(list)
                .total(list.size())
                .build();
    }

    public void reloadDatabase(String databaseName){
        if (!connectDatabase || (StringUtils.isNotBlank(dbName) && !dbName.equals(databaseName)) ){
            List<DatabaseReq> databaseList = druidService.getDatabaseList();
            if (StringUtils.isEmpty(databaseName)){
                databaseName = databaseList.get(0).getName();
            }
            dbName = databaseName;
            String finalDatabaseName = databaseName;
            Optional<DatabaseReq> first = databaseList.stream().filter(m -> m.getName().equals(finalDatabaseName)).findFirst();
            DatabaseReq databaseReq = null;
            if (first.isPresent()){
                databaseReq = first.get();
            }
            if (databaseReq!=null) {
                connectDatabase = true;
                druidService.reload(databaseReq);
            }
        }

    }


    public TableEntity queryTable(String tableName) {
        return baseMapper.queryTable(tableName);
    }

    public List<ColumnEntity> queryColumns(String tableName) {
        return baseMapper.queryColumns(tableName);
    }

    public byte[] generatorZip(CodeReq req){

        reloadDatabase(req.getDatabaseName());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);


        String[] tableNames=req.getGenerateTableNames().split(",");
        for(String tableName : tableNames){
            Map<String,StringWriter> fileMap = generator(req,tableName);
            if (CollectionUtils.isNotEmpty(fileMap)){
                createZip(zip,fileMap);
            }
        }

        if ("on".equals(req.getFixed())) {
            Map<String, StringWriter> fixedMap = generateFixed(req);
            if (CollectionUtils.isNotEmpty(fixedMap)) {
                createZip(zip, fixedMap);
            }
        }

        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    public void generatorCode(CodeReq req) {

        reloadDatabase(req.getDatabaseName());

        String[] tableNames=req.getGenerateTableNames().split(",");
        for(String tableName : tableNames){
            Map<String,StringWriter> fileMap = generator(req,tableName);
            if (CollectionUtils.isNotEmpty(fileMap)){
                for(Map.Entry<String,StringWriter> map: fileMap.entrySet()){

                    createFile(map.getKey(),map.getValue().toString());
                }
            }
        }

        if ("on".equals(req.getFixed())){
            Map<String, StringWriter> fixedMap = generateFixed(req);
            if (CollectionUtils.isNotEmpty(fixedMap)){
                for(Map.Entry<String,StringWriter> map: fixedMap.entrySet()){
                    createFile(map.getKey(),map.getValue().toString());
                }
            }
        }


    }

    private boolean connectDatabase =  false;
    private String dbName = "";

    private Map<String,StringWriter> generator(CodeReq req,String tableName){


        TableEntity tableEntity = queryTable(tableName);
        if (tableEntity==null){
            throw new QuickException(tableName+"??????????????????????????????");
        }

        String className = tableToJava(tableEntity.getTableName(), req.getExcludePrefix());
        tableEntity.setClassName(className);
        tableEntity.setVariableClassName(StringUtils.uncapitalize(className));

        boolean hasBigDecimal = false;

        List<ColumnEntity> columns = queryColumns(tableName);
        setDataType(tableEntity, hasBigDecimal, columns,req);
        tableEntity.setColumns(columns);

        hasBigDecimal = req.isHasBigDecimal();

        //???????????????????????????????????????
        if(tableEntity.getPrimaryKeyColumnEntity() == null){
            tableEntity.setPrimaryKeyColumnEntity(tableEntity.getColumns().get(0));
        }

        //??????velocity???????????????
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);



        //??????????????????
        VelocityContext velocityContext = initTemplate(req, tableEntity, className, hasBigDecimal);


        Map<String,StringWriter> fileMap = generateTemplate( velocityContext, mapConfig.getUnset() );

        return fileMap;
    }

    private Map<String,StringWriter> generateFixed(CodeReq req){
        VelocityContext velocityContext = initTemplate(req);
        Map<String,StringWriter> fileMap = new HashMap<>();
        List<String> list = mapConfig.getFixed();

        String out =(String) velocityContext.get("out");
        String packageOut =replaceAll((String)velocityContext.get("package"));

        if (CollectionUtils.isNotEmpty(list)){
            for (int i = 0; i < list.size() ; i++) {
                String vm = list.get(i);
                String[] vms = vm.split(",");
                String fileName = vms[0];
                String lasePackage = vms[1];
                String vmPath = vms[2];

                //????????????
                StringWriter sw = getStringWriter(vmPath, velocityContext);
                String javaFileNamePath =out+SRC_MAIN_JAVA+File.separator+packageOut+File.separator+ lasePackage +File.separator+ fileName;
                fileMap.put(javaFileNamePath,sw);
            }
        }
        if ("on".equals(req.getFixed())) {



            if ("maven".equals(req.getCompileType())) {
                //????????????
                StringWriter sw = getStringWriter("vm/fixed/pom.xml.vm", velocityContext);
                String javaFileNamePath = out + File.separator  + "pom.xml";
                fileMap.put(javaFileNamePath, sw);
            } else if ("gradle".equals(req.getCompileType())) {
                //????????????
                StringWriter sw = getStringWriter("vm/fixed/build.gradle.vm", velocityContext);
                String javaFileNamePath = out + File.separator  + "build.gradle";
                fileMap.put(javaFileNamePath, sw);

                StringWriter sw2 = getStringWriter("vm/fixed/settings.gradle.vm", velocityContext);
                String javaFileNamePath2 = out + File.separator  + "settings.gradle";
                fileMap.put(javaFileNamePath2, sw2);
            }


            List<String> rootList = mapConfig.getRoot();
            if (CollectionUtils.isNotEmpty(rootList)){
                for (String ff: rootList) {
                    String[] ss = ff.split(",");
                    if (ss!=null && ss.length==3){
                        String fileName = ss[0];
                        String packagePath = ss[1];
                        String vmPath = ss[2];
                        if (StringUtils.isEmpty(fileName) || StringUtils.isEmpty(vmPath)){
                            continue;
                        }

                        StringWriter sw2 = getStringWriter(vmPath, velocityContext);
                        String javaFileNamePath2 = out + File.separator +packagePath+File.separator + fileName;
                        fileMap.put(javaFileNamePath2, sw2);
                    }
                }
            }

            List<String> resourceList =  mapConfig.getResource();
            if (CollectionUtils.isNotEmpty(resourceList)){
                for (String ff: resourceList) {
                    String[] ss = ff.split(",");

                    if (ss!=null && ss.length==3) {
                        String fileName = ss[0];
                        String packagePath = ss[1];
                        String vmPath = ss[2];
                        if (StringUtils.isEmpty(fileName) || StringUtils.isEmpty(vmPath)) {
                            continue;
                        }

                        StringWriter sw2 = getStringWriter(vmPath, velocityContext);
                        String javaFileNamePath2 = out + SRC_MAIN_RESOURCES + File.separator + packagePath + File.separator + fileName;
                        fileMap.put(javaFileNamePath2, sw2);
                    }
                }
            }

        }

        return fileMap;
    }

    private StringWriter getStringWriter(String name, VelocityContext velocityContext) {
        StringWriter sw = new StringWriter();
        Template tpl = Velocity.getTemplate(name, "UTF-8");
        tpl.merge(velocityContext, sw);
        return sw;
    }


    private Map<String,StringWriter> generateTemplate( VelocityContext context, Map<String,String > vmMap){

        Map<String,StringWriter> fileMap = new HashMap<>();

        String out =(String) context.get("out");
        String packageOut =replaceAll((String)context.get("package"));

        for(Map.Entry<String,String> map: vmMap.entrySet()){
            String path  = (String) context.get(map.getKey());
            String vm = map.getValue();
            if (StringUtils.isEmpty(path)){
                log.info("vm:"+vm+" ?????????????????????");
                continue;
            }
            path = replaceAll(path);


            //????????????
            StringWriter sw = getStringWriter(vm, context);
            //?????????zip
            //???????????????
            String className = (String) context.internalGet("className");
            String fileName =out+SRC_MAIN_JAVA+File.separator+packageOut+File.separator+ path +File.separator+ className+ vm.replaceAll(".vm","").replaceAll("vm/","");

            fileMap.put(fileName,sw);
        }

        return fileMap;
    }

    private String replaceAll(String str){
        return str.replaceAll("\\.", Matcher.quoteReplacement(File.separator))
                .replaceAll("/",Matcher.quoteReplacement(File.separator))
                .replaceAll("\\\\",Matcher.quoteReplacement(File.separator));
    }

    public void createZip(ZipOutputStream zip,Map<String,StringWriter> fileMap){
        if (CollectionUtils.isNotEmpty(fileMap)){
            for (Map.Entry<String,StringWriter> map: fileMap.entrySet()) {
                String key = map.getKey();
                StringWriter value = map.getValue();

                try {
                    ZipEntry zipEntry = new ZipEntry(key);
                    zip.putNextEntry(zipEntry);
                    IOUtils.write(value.toString(), zip, "UTF-8");
                    IOUtils.closeQuietly(value);
                    zip.closeEntry();

                    log.info("file:"+key+" ????????????");
                }catch (Exception e){
                    log.error("create zip error",e);
                }
            }
        }
    }


    public void createFile(String fileName,String fileContent) {
        try {
            File file=new File(fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if(!file.exists()) {
                file.createNewFile();
            }else{
                file.delete();
            }
            FileOutputStream out=new FileOutputStream(file,true);
            out.write(fileContent.getBytes("utf-8"));
            out.close();
            log.info("fileName:"+fileName+" ????????????");
        }catch (Exception e){
            log.error("createFile error",e);
        }

    }

    //??????????????????
    private VelocityContext initTemplate(CodeReq req, TableEntity tableEntity, String className, boolean hasBigDecimal) {

        Map<String, Object> map = new HashMap<>();
        map.put("out",req.getOut());
        map.put("package",req.getPackageOut());

        map.put("pagehelperStatus","on".equals(req.getPagehelperStatus()));
        map.put("mapperXmlAliasStatus","on".equals(req.getMapperXmlAliasStatus()));
        if (StringUtils.isNotEmpty(req.getReqOut())) {
            map.put("reqOut", decode(req.getReqOut()));
        }
        if (StringUtils.isNotEmpty(req.getControllerOut())) {
            map.put("controllerOut", decode(req.getControllerOut()));
        }
        if (StringUtils.isNotEmpty(req.getServiceOut())){
            map.put("serviceOut",decode(req.getServiceOut()));
            map.put("serviceImplOut",decode(req.getServiceOut())+Matcher.quoteReplacement(File.separator)+"impl"+Matcher.quoteReplacement(File.separator));
        }
        if (StringUtils.isNotEmpty(req.getEntityOut())) {
            map.put("entityOut", decode(req.getEntityOut()));
        }
        if (StringUtils.isNotEmpty(req.getMapperOut())) {
            map.put("mapperOut", decode(req.getMapperOut()));
        }
        if (StringUtils.isNotEmpty(req.getMapperXmlOut())) {
            map.put("mapperXmlOut", decode(req.getMapperXmlOut()));
        }

        map.put("excludePrefix",decode(req.getExcludePrefix()));
        map.put("author",req.getAuthor());
        map.put("email",req.getEmail());
        map.put("swaggerStatus","on".equals(req.getSwaggerStatus()));
        map.put("dtoStatus","on".equals(req.getDtoStatus()));
        map.put("insertsStatus","on".equals(req.getInsertsStatus()));
        if ("on".equals(req.getDtoStatus())){
            map.put("dtoOut",".dto");
        }

        map.put("oldTableName", tableEntity.getTableName());
        // ????????????
        if(StringUtils.isNotBlank(tableEntity.getComments())){
            map.put("comments", tableEntity.getComments());
            map.put("commentsEntity", tableEntity.getComments());
            map.put("commentsService", tableEntity.getComments());
            map.put("commentsController", tableEntity.getComments());
            map.put("commentsApi", tableEntity.getComments());
            map.put("commentsDao", tableEntity.getComments());
        }
        map.put("pk", tableEntity.getPrimaryKeyColumnEntity());
        map.put("pkAttrType",tableEntity.getPrimaryKeyColumnEntity()!=null?tableEntity.getPrimaryKeyColumnEntity().getAttrType():"String");
        map.put("className", tableEntity.getClassName().replace(captureName(req.getExcludePrefix()),""));
        map.put("variableClassName", tableEntity.getVariableClassName());
        map.put("columns", tableEntity.getColumns());


        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (ColumnEntity c : tableEntity.getColumns()) {
            if (StringUtils.isNotBlank(sb.toString())) {
                sb.append(",").append(c.getColumnName());
                sb2.append(",a.").append(c.getColumnName());
            } else {
                sb.append(c.getColumnName());
                sb2.append("a." + c.getColumnName());
            }
        }
        map.put("allColumnNames",sb.toString());
        map.put("allColumnNames2",sb2.toString());

        map.put("hasBigDecimal", hasBigDecimal);

        // API????????????
        if(tableEntity.getCreateTime()!= null){
            map.put("apiSort", StringUtils.substring(String.valueOf(tableEntity.getCreateTime().getTime() + System.currentTimeMillis()), 1, 9));
        }


        if(StringUtils.isNotBlank(tableEntity.getTableName())){
            String tableName = StringUtils.lowerCase(tableEntity.getTableName());
            map.put("pathName", tableName.replace("_","/"));
            map.put("permissionPrefix", tableEntity.getVariableClassName());
            map.put("classServiceImpl",tableEntity.getVariableClassName());
            map.put("requestMappingPath",tableEntity.getVariableClassName());
            // ??????????????????
            map.put("apiPermission", tableName.replace("_",":"));
            // className.toLowerCase()
            map.put("vueFileName", className.toLowerCase());
        }

        map.put("datetime", DateFormatUtils.format(new Date(),"yyyy-MM-dd HH:mm:ss"));
        map.put("date", DateFormatUtils.format(new Date(),"yyyy-MM-dd"));
        map.put("projectName","example");

//        map.put("package2",map.get("package").toString().replace(".","/"));
        VelocityContext context = new VelocityContext(map);

        return context;
    }


    //??????????????????
    private VelocityContext initTemplate(CodeReq req) {

        Map<String, Object> map = new HashMap<>();
        map.put("out",req.getOut());
        map.put("package",req.getPackageOut());
        map.put("pagehelperStatus","on".equals(req.getPagehelperStatus()));
        map.put("mapperXmlAliasStatus","on".equals(req.getMapperXmlAliasStatus()));
        if (StringUtils.isNotEmpty(req.getReqOut())) {
            map.put("reqOut", decode(req.getReqOut()));
        }
        if (StringUtils.isNotEmpty(req.getControllerOut())) {
            map.put("controllerOut", decode(req.getControllerOut()));
        }
        if (StringUtils.isNotEmpty(req.getServiceOut())){
            map.put("serviceOut",decode(req.getServiceOut()));
            map.put("serviceImplOut",decode(req.getServiceOut())+Matcher.quoteReplacement(File.separator)+"impl"+Matcher.quoteReplacement(File.separator));
        }
        if (StringUtils.isNotEmpty(req.getEntityOut())) {
            map.put("entityOut", decode(req.getEntityOut()));
        }
        if (StringUtils.isNotEmpty(req.getMapperOut())) {
            map.put("mapperOut", decode(req.getMapperOut()));
        }
        if (StringUtils.isNotEmpty(req.getMapperXmlOut())) {
            map.put("mapperXmlOut", decode(req.getMapperXmlOut()));
        }

        map.put("excludePrefix",decode(req.getExcludePrefix()));
        map.put("author",req.getAuthor());
        map.put("email",req.getEmail());
        map.put("swaggerStatus","on".equals(req.getSwaggerStatus()));
        map.put("dtoStatus","on".equals(req.getDtoStatus()));
        map.put("insertsStatus","on".equals(req.getInsertsStatus()));
        if ("on".equals(req.getDtoStatus())){
            map.put("dtoOut",".dto");
        }
        map.put("datetime", DateFormatUtils.format(new Date(),"yyyy-MM-dd HH:mm:ss"));
        map.put("date", DateFormatUtils.format(new Date(),"yyyy-MM-dd"));
        map.put("projectName","example");
        VelocityContext context = new VelocityContext(map);
        return context;
    }

    private String decode(String path){
        try {
            return URLDecoder.decode(path,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }


    private void setDataType(TableEntity tableEntity, boolean hasBigDecimal, List<ColumnEntity> columns,CodeReq req) {


        String keyword = environment.getProperty("mars.quick.keyword");
        String[] keywords = keyword.split(",");

        for(ColumnEntity columnEntity: columns){
            //???????????????Java?????????
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setVariableAttrName(StringUtils.uncapitalize(attrName));

            columnEntity.setColumnNameXmlUse(columnEntity.getColumnName());
            if (Arrays.stream(keywords).filter(m-> m.equals(columnEntity.getColumnName())).count()>0){
                columnEntity.setColumnName("`"+columnEntity.getColumnName()+"`");
            }

            //??????????????????????????????Java??????
            if("NUMBER".equals(columnEntity.getDataType()) && columnEntity.getDataScale() > 0){
                columnEntity.setAttrType("Double");
            }else if("NUMBER".equals(columnEntity.getDataType()) && columnEntity.getDataPrecision()>14){
                columnEntity.setAttrType("Long");
            }else{
                String attrType = environment.getProperty(columnEntity.getDataType(), "unknowType");
                columnEntity.setAttrType(attrType);
                if (!hasBigDecimal && attrType.equals("BigDecimal" )) {
                    req.setHasBigDecimal(true);
                }
            }
            //????????????
            if("PRI".equalsIgnoreCase(columnEntity.getColumnKey()) && tableEntity.getPrimaryKeyColumnEntity() == null){
                tableEntity.setPrimaryKeyColumnEntity(columnEntity);
            }
        }
    }


    /**
     * ?????????????????????????????????
     * @param str ????????????????????????
     * @return
     */
    private static String captureName(String str) {
        if (StringUtils.isNotBlank(str)) {
            // ???????????????ascii??????????????????????????????????????????????????????????????????
            char[] cs = str.toCharArray();
            cs[0] -= 32;
            return String.valueOf(cs);
        }
        return "";
    }

    /**
     * ???????????????Java??????
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if(StringUtils.isNotBlank(tablePrefix)){
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    /**
     * ???????????????Java?????????
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }
}
