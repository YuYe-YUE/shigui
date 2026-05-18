package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** match_record 表，AI 智能匹配结果 */
@Data
@TableName("match_record")
public class MatchRecord {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 自增主键
    private Long lostPostId;         // 失物单据 ID
    private Long foundPostId;        // 招领单据 ID
    private BigDecimal score;        // 匹配得分（0~1）
    private String reason;           // AI 匹配理由
    @TableLogic
    private Integer deleted;         // 逻辑删除标记
    private LocalDateTime createdAt; // 匹配时间
}
