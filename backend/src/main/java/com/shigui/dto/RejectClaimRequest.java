package com.shigui.dto;

import lombok.Data;

/** 拒绝认领请求——含管理员驳回原因 */
@Data
public class RejectClaimRequest {
    private String reason;
}
