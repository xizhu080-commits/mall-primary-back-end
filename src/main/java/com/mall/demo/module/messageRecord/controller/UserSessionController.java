package com.mall.demo.module.messageRecord.controller;


import com.mall.demo.common.result.RestResp;
import com.mall.demo.module.messageRecord.dto.req.CreateSessionReqDto;
import com.mall.demo.module.messageRecord.dto.resp.CreateSessionRespDto;
import com.mall.demo.module.messageRecord.service.UserSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/userSession")
public class UserSessionController {



    private final UserSessionService userSessionService;

    /*
    * 创建会话:联系客服
    * */
    @PostMapping("/createSession")
    public RestResp<CreateSessionRespDto> createSession(@Valid @RequestBody CreateSessionReqDto dto) {
       return RestResp.ok(userSessionService.createSession(dto));

    }

}
