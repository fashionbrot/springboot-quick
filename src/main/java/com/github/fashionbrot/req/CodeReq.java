package com.github.fashionbrot.req;

import lombok.Data;

@Data
public class CodeReq {

    /**
     * zip  local
     */
    private String outType;

    private String packageOut;

    private String out;
    private String controllerOut;
    private String serviceOut;
    private String entityOut;
    private String mapperOut;
    private String mapperXmlOut;
    private String reqOut;

    private String  generateTableNames;
    private String excludePrefix;
    private String author;
    private String email;
    private String swaggerStatus;
    private String dtoStatus;
    private String insertsStatus;

    private boolean hasBigDecimal;

    private String databaseName;

    /**
     * 是否生成 表之外的固定文件
     */
    private String fixed;

    private String compileType;

    private String pagehelperStatus;

    private String mapperXmlAliasStatus;
}
