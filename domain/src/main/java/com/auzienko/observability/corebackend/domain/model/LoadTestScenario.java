package com.auzienko.observability.corebackend.domain.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class LoadTestScenario {

    private String name;
    private int durationSeconds;
    private int virtualUsers;
    private List<Step> steps;

}
