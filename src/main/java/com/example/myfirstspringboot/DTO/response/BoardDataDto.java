package com.example.myfirstspringboot.DTO.response;

import lombok.Data;
import java.util.List;

@Data
public class BoardDataDto {

    private BoardDto board;
    private List<ColumnDto> columns;
    private List<JobCardDto> cards;
}
