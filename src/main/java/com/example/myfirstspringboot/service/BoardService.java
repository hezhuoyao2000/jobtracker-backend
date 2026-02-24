package com.example.myfirstspringboot.service;

import com.example.myfirstspringboot.dto.request.CreateBoardRequest;
import com.example.myfirstspringboot.dto.request.LoadBoardRequest;
import com.example.myfirstspringboot.dto.response.BoardDataDto;
import com.example.myfirstspringboot.dto.response.BoardDto;


public interface BoardService {
    /**
     * 加载看板完整数据
     * @param userId 当前用户 ID
     * @param request 请求参数（包含可选的 boardId）
     * @return 看板完整数据（board + columns + cards）
     */
    BoardDataDto loadBoard(String userId, LoadBoardRequest request);

    /**
     * 创建看板
     * @param userId 当前用户 ID
     * @param request 请求参数（包含看板名称）
     * @return 创建后的看板信息
     */
    BoardDto createBoard(String userId, CreateBoardRequest request);

}
