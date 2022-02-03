package com.github.fashionbrot.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseReq {

    private String name;

    private String driverClassName;

    private String url;

    private String username;

    private String password;

}
