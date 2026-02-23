package com.example.myfirstspringboot.util;

import com.example.myfirstspringboot.Entity.Board;
import com.example.myfirstspringboot.Entity.KanbanColumn;
import com.example.myfirstspringboot.Entity.JobCard;
import com.example.myfirstspringboot.DTO.response.BoardDto;
import com.example.myfirstspringboot.DTO.response.ColumnDto;
import com.example.myfirstspringboot.DTO.response.JobCardDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DtoConverter {

    /**
     * Board Entity 转 BoardDto
     */
    public BoardDto toBoardDto(Board board) {
        if (board == null) {
            return null;
        }
        BoardDto dto = new BoardDto();
        dto.setId(board.getId());
        dto.setUserId(board.getUserId());
        dto.setName(board.getName());
        if (board.getCreatedAt() != null) {
            dto.setCreatedAt(board.getCreatedAt().toString());
        }
        if (board.getUpdatedAt() != null) {
            dto.setUpdatedAt(board.getUpdatedAt().toString());
        }
        return dto;
    }

    /**
     * KanbanColumn Entity 转 ColumnDto
     */
    public ColumnDto toColumnDto(KanbanColumn column) {
        if (column == null) {
            return null;
        }
        ColumnDto dto = new ColumnDto();
        dto.setId(column.getId());
        dto.setBoardId(column.getBoardId());
        dto.setName(column.getName());
        dto.setSortOrder(column.getSortOrder());
        dto.setIsDefault(column.getIsDefault());
        dto.setCustomAttributes(column.getCustomAttributes());
        return dto;
    }

    /**
     * JobCard Entity 转 JobCardDto
     */
    public JobCardDto toJobCardDto(JobCard jobCard) {
        if (jobCard == null) {
            return null;
        }
        JobCardDto dto = new JobCardDto();
        dto.setId(jobCard.getId());
        dto.setBoardId(jobCard.getBoardId());
        dto.setStatusId(jobCard.getStatusId());
        dto.setJobTitle(jobCard.getJobTitle());
        dto.setCompanyName(jobCard.getCompanyName());
        dto.setJobLink(jobCard.getJobLink());
        dto.setSourcePlatform(jobCard.getSourcePlatform());
        dto.setExpired(jobCard.getExpired());
        dto.setJobLocation(jobCard.getJobLocation());
        dto.setDescription(jobCard.getDescription());
        if (jobCard.getAppliedTime() != null) {
            dto.setAppliedTime(jobCard.getAppliedTime().toString());
        }
        dto.setTags(jobCard.getTags());
        dto.setComments(jobCard.getComments());
        dto.setExtra(jobCard.getExtra());
        if (jobCard.getCreatedAt() != null) {
            dto.setCreatedAt(jobCard.getCreatedAt().toString());
        }
        if (jobCard.getUpdatedAt() != null) {
            dto.setUpdatedAt(jobCard.getUpdatedAt().toString());
        }
        return dto;
    }

    /**
     * 批量转换
     */
    public List<BoardDto> toBoardDtoList(List<Board> boards) {
        return boards.stream().map(this::toBoardDto).collect(Collectors.toList());
    }

    public List<ColumnDto> toColumnDtoList(List<KanbanColumn> columns) {
        return columns.stream().map(this::toColumnDto).collect(Collectors.toList());
    }

    public List<JobCardDto> toJobCardDtoList(List<JobCard> jobCards) {
        return jobCards.stream().map(this::toJobCardDto).collect(Collectors.toList());
    }
}
