package com.example.myfirstspringboot.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class UpdateCardRequest {

    private UUID cardId;
    private UUID statusId;
    private String jobTitle;
    private String companyName;
    private String jobLink;
    private String sourcePlatform;
    private String jobLocation;
    private String description;
    private List<String> tags;
    private String comments;
    private Map<String, Object> extra;
}
