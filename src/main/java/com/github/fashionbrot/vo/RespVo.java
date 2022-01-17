package com.github.fashionbrot.vo;


import com.github.fashionbrot.enums.RespEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
* 统一返回vo 类
*/
@Data
@Builder
@AllArgsConstructor
public class RespVo<T> implements Serializable{
    private static final long serialVersionUID = -3655390020082644681L;

    public static final int SUCCESS = RespEnum.SUCCESS.getCode();
    public static final int FAILED = RespEnum.FAIL.getCode();
    public static final RespVo vo = RespVo.success(null);

    private int code;
    private String msg;
    private T data;


    public static RespVo fail(String msg){
        return RespVo.builder().code(FAILED).msg(msg).build();
    }

    public static RespVo fail(String msg, int code){
        return RespVo.builder().code(code).msg(msg).build();
    }

    public static<T> RespVo success(T data){
        return RespVo.builder().code(SUCCESS).msg("成功").data(data).build();
    }

    public static RespVo success(){
        return vo;
    }

    public static RespVo respCode(RespEnum respCode){
        return RespVo.builder().code(respCode.getCode()).msg(respCode.getMsg()).build();
    }

}
