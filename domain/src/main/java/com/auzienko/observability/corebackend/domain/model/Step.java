package com.auzienko.observability.corebackend.domain.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class Step {

    private String name;
    private HttpRequest request;
    private Map<String, String> extract;

}
