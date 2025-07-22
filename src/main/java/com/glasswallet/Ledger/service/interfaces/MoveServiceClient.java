package com.glasswallet.Ledger.service.interfaces;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.dtos.response.SuiResponse;
import org.springframework.stereotype.Component;

@Component
public interface MoveServiceClient {
    SuiResponse logOnChain(LedgerEntry entry);
}
