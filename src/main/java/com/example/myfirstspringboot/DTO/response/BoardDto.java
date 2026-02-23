package com.example.myfirstspringboot.DTO.response;

import lombok.Data;
import java.util.UUID;

@Data
public class BoardDto {

    private UUID id;
    private String userId;
    private String name;
    private String createdAt;
    private String updatedAt;
}
