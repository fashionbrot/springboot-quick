package com.github.fashionbrot.exception;

import com.github.fashionbrot.vo.RespVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(QuickException.class)
    public RespVo handleRRException(QuickException e){
        log.info(e.getMsg());
        return RespVo.builder()
                .code(e.getCode())
                .msg(e.getMsg())
                .build();
    }

    @ExceptionHandler(Exception.class)
    public RespVo handleException(Exception e){
        log.error(e.getMessage(), e);
        return RespVo.builder()
                .code(RespVo.FAILED)
                .msg("未知异常")
                .build();
    }

}
