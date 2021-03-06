package com.github.fashionbrot.controller;


import com.github.fashionbrot.entity.ColumnEntity;
import com.github.fashionbrot.req.CodeReq;
import com.github.fashionbrot.req.PageReq;
import com.github.fashionbrot.service.QuickService;
import com.github.fashionbrot.vo.RespVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
@Slf4j
@Controller
public class IndexController {


    @Autowired
    private QuickService quickService;
    @Autowired
    private HttpServletResponse response;

    @GetMapping("/")
    public String index(){
        return "index";
    }



    /**
     * 列表
     */
    @ResponseBody
    @RequestMapping("/list")
    public RespVo list(PageReq req){
        return RespVo.success(quickService.queryList(req));
    }
    /**
     * 字段列表
     */
    @ResponseBody
    @RequestMapping("/columnList")
    public RespVo columnList(@RequestParam Map<String, Object> params){
        List<ColumnEntity> columnList = quickService.queryColumns(params.get("tableName").toString());

        return RespVo.builder()
                .data(columnList)
                .build();
    }




    /**
     * 生成代码 下载文件
     */
    @ResponseBody
    @RequestMapping("/generateZip")
    public void generateZip(CodeReq req) throws IOException {

        byte[] data = quickService.generatorZip( req);
        try {
            response.reset();
            response.setHeader("Content-Disposition", ("attachment; filename=\"quick.zip\""));
            response.addHeader("Content-Length", "" + data.length);
            response.setContentType("application/octet-stream; charset=UTF-8");
            IOUtils.write(data, response.getOutputStream());
        }catch (Exception e){
            log.error("generateZip error",e);
        }

    }



    /**
     * 生成代码 到本地
     */
    @ResponseBody
    @RequestMapping("/generate")
    public RespVo generate(CodeReq req) throws IOException {
        quickService.generatorCode( req);
        return RespVo.success();
    }


}
