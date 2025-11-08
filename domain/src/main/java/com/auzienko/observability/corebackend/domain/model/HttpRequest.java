package com.auzienko.observability.corebackend.domain.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class HttpRequest {

    private String method;
    private String url;
    private Map<String, String> headers;
    private Map<String, Object> body;

}
