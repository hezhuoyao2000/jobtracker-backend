package com.example.myfirstspringboot.DTO.request;

import lombok.Data;
import java.util.UUID;

@Data
public class DeleteCardRequest {

    private UUID cardId;
}
