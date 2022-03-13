package com.github.fashionbrot;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import java.util.UUID;

public class Test {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
//            System.out.println(NanoIdUtils.randomNanoId());
            System.out.println(UUID.randomUUID().toString());
        }
        System.out.println(System.currentTimeMillis()-start);
    }
}
