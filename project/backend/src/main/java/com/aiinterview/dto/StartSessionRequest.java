package com.aiinterview.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartSessionRequest {
    private String candidateName;
    private String jobRole;
    private String resumeText;
}
