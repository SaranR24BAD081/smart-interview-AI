package com.aiinterview.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeSubmitRequest {
    private Long submissionId;
    private String code;
    private String language = "javascript";
}
