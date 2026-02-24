package com.example.myfirstspringboot.dto.request;

import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class UpdateColumnRequest {

    private UUID columnId;
    private String name;
    private Integer sortOrder;
    private Map<String, Object> customAttributes;
}
