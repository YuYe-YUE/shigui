package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("claim_record")
public class ClaimRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long claimantUserId;
    private String privateFeatureAnswer;
    private String status;
    private String aiDecision;
    private BigDecimal aiConfidence;
    private String aiReason;
    private String adminReason;
    private LocalDateTime verifiedAt;
    private LocalDateTime completedAt;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
