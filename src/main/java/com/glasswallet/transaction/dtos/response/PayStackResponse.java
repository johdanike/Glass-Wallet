package com.glasswallet.transaction.dtos.response;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PayStackResponse {
    private boolean status;
    private String message;
    private Data data;



    @lombok.Data
    public static class Data {
        private String authorization_url;
        private String access_code;
        private String reference;
    }
}
