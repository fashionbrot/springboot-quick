package com.github.fashionbrot.req;

import lombok.Data;

@Data
public class CodeReq {

    /**
     * zip  local
     */
    private String outType;

    private String out;
    private String controllerOut;
    private String serviceOut;
    private String entityOut;
    private String mapperOut;
    private String mapperXmlOut;

    private String  generateTableNames;
    private String excludePrefix;
    private String author;
    private String email;
    private String swaggerStatus;
    private String dtoStatus;


}
