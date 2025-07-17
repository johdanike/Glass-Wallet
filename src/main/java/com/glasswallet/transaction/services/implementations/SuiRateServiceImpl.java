package com.glasswallet.transaction.services.implementations;

import com.glasswallet.transaction.services.interfaces.SuiRateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SuiRateServiceImpl implements SuiRateService {
    
    @Override
    public BigDecimal getSuiToNgnRate() {
        // TODO: Implement actual rate fetching from external API
        // For now, return a fixed rate
        return new BigDecimal("1500.00");
    }
}