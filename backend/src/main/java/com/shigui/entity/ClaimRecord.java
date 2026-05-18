package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** claim_record 表，用户认领申请记录 */
@Data
@TableName("claim_record")
public class ClaimRecord {
    @TableId(type = IdType.AUTO)
    private Long id;                        // 自增主键
    private Long postId;                    // 关联单据 ID
    private Long claimantUserId;            // 认领人用户 ID
    private String privateFeatureAnswer;    // 对私密特征的回答
    private String status;                  // 认领状态：PENDING/APPROVED/REJECTED/CONFIRMED
    private String aiDecision;              // AI 预审决策：APPROVED/REJECTED/UNCERTAIN
    private BigDecimal aiConfidence;        // AI 预审置信度（0~1）
    private String aiReason;                // AI 预审理由
    private String adminReason;             // 管理员审核备注
    private LocalDateTime verifiedAt;       // 管理员审核时间
    private LocalDateTime completedAt;      // 确认收到时间
    @TableLogic
    private Integer deleted;                // 逻辑删除标记
    private LocalDateTime createdAt;        // 创建时间
    private LocalDateTime updatedAt;        // 更新时间
}
