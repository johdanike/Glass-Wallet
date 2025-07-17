package com.glasswallet.Wallet.utils;

import com.glasswallet.Wallet.enums.WalletCurrency;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletUtils {

    public static String generateAccountNumberFromPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        String clean = phoneNumber.replaceAll("\\D", "");
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
        }
        return clean.length() >= 10
                ? clean.substring(clean.length() - 10)
                : String.format("%010d", Long.parseLong(clean));
    }

    public static String getCountryCodeFromPhoneNumber(String phoneNumber) {
        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber parsed = phoneUtil.parse(phoneNumber, null);
            if (phoneUtil.isValidNumber(parsed)) {
                String regionCode = phoneUtil.getRegionCodeForNumber(parsed);
                return regionCode != null ? regionCode : "NG";
            }
        } catch (Exception e) {
            log.warn("Failed to resolve country code, defaulting to NG: {}", e.getMessage());
        }
        return "NG";
    }

    public static WalletCurrency resolveCurrencyFromCountryCode(String code) {
        return switch (code.toUpperCase()) {
            case "NG" -> WalletCurrency.NGN;
            default -> WalletCurrency.NGN;
        };
    }

    public static WalletCurrency resolveCurrencyFromPhoneNumber(String phoneNumber) {
        String countryCode = getCountryCodeFromPhoneNumber(phoneNumber);
        return resolveCurrencyFromCountryCode(countryCode);
    }
}
