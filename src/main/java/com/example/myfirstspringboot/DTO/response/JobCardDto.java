package com.example.myfirstspringboot.DTO.response;

import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class JobCardDto {

    private UUID id;
    private UUID boardId;
    private UUID statusId;
    private String jobTitle;
    private String companyName;
    private String jobLink;
    private String sourcePlatform;
    private Boolean expired;
    private String jobLocation;
    private String description;
    private String appliedTime;
    private List<String> tags;
    private String comments;
    private Map<String, Object> extra;
    private String createdAt;
    private String updatedAt;
}
