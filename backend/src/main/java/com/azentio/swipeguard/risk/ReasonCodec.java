package com.azentio.swipeguard.risk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Component;

/** Reasons are stored as a JSON text column so the same schema works on PostgreSQL and H2. */
@Component
public class ReasonCodec {

    private static final TypeReference<List<Reason>> REASON_LIST = new TypeReference<>() { };

    private final ObjectMapper mapper;

    public ReasonCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String toJson(List<Reason> reasons) {
        try {
            return mapper.writeValueAsString(reasons);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot write reasons", e);
        }
    }

    public List<Reason> fromJson(String json) {
        try {
            return mapper.readValue(json, REASON_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read reasons", e);
        }
    }
}