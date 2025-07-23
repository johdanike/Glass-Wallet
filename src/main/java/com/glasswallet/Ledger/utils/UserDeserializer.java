package com.glasswallet.Ledger.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.glasswallet.user.data.models.User;

import java.io.IOException;
import java.util.UUID;

public class UserDeserializer extends JsonDeserializer<User> {
    @Override
    public User deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String userId = p.getText();
        User user = new User();
        user.setId(UUID.fromString(userId));
        return user;
    }
}

