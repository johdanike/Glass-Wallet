package com.glasswallet.Ledger.controllers;

import com.glasswallet.Ledger.service.interfaces.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {
    private final LedgerService ledgerService;


}
