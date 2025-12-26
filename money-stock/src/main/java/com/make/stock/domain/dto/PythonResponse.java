package com.make.stock.domain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PythonResponse<T> {
    private T data;
    private Meta meta;

    @Data
    public static class Meta {
        private String provider;
        private String quality;
        private List<Map<String, Object>> trace;
    }
}
