package com.glasswallet.fiat.dtos.responses;


import com.glasswallet.fiat.data.model.DataObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PayStackResponse {
    private boolean status;
    private String message;
    private DataObject data;



}
