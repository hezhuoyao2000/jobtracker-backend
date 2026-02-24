package com.example.myfirstspringboot.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class MoveCardRequest {

    private UUID cardId;
    private UUID targetStatusId;
}
