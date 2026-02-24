package com.example.myfirstspringboot.dto.response;

import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class ColumnDto {

    private UUID id;
    private UUID boardId;
    private String name;
    private Integer sortOrder;
    private Boolean isDefault;
    private Map<String, Object> customAttributes;
}
