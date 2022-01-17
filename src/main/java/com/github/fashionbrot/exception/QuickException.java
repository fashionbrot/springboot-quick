package com.github.fashionbrot.exception;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class QuickException extends RuntimeException  {

    private int code=-1;

    private String msg;


    public QuickException(String msg) {
        super(msg);
        this.msg = msg;
    }
}
