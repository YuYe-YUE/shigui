package com.shigui.dto;

import lombok.Data;

/** 发起认领请求——包含单据 ID 和私密特征回答 */
@Data
public class CreateClaimRequest {
    private Long postId;
    private String privateFeatureAnswer;
}
