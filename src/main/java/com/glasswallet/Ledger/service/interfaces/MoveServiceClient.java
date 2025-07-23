package com.glasswallet.Ledger.service.interfaces;

import com.glasswallet.Ledger.data.model.LedgerEntry;
import com.glasswallet.Ledger.dtos.responses.SuiResponse;

public interface MoveServiceClient {
    SuiResponse logOnChain(LedgerEntry entry);
}
