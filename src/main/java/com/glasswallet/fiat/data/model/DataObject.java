package com.glasswallet.fiat.data.model;

import lombok.Data;

@Data
public class DataObject {
    private String authorization_url;
    private String access_code;
    private String reference;
}
