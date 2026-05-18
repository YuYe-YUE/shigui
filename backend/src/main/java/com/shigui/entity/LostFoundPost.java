package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** lost_found_post 表，失物/招领核心单据 */
@Data
@TableName("lost_found_post")
public class LostFoundPost {
    @TableId(type = IdType.AUTO)
    private Long id;                       // 自增主键
    private Long userId;                   // 发布者用户 ID
    private String postType;               // 单据类型：LOST/FOUND
    private String title;                  // 标题
    private String itemName;               // 物品名称
    private String itemCategory;           // 物品分类
    private String description;            // 物品描述
    private String privateFeature;         // 私密特征（认领验证用，不公开）
    private String campusArea;             // 校区
    private String locationName;           // 具体地点
    private Double longitude;              // 经度
    private Double latitude;               // 纬度
    private String storageLocation;        // 保管地点（仅招领单填写）
    private LocalDateTime eventTime;       // 丢失/拾取时间
    private String status;                 // 单据状态流转：PENDING_AUDIT/MATCHING/CLAIMING/RETURNING/COMPLETED
    private LocalDateTime publishedAt;     // 发布时间
    // 不设 @TableLogic，deleted=1 的记录管理端仍需可见；公开接口里显式 eq(deleted, 0)
    private Integer deleted;               // 删除标记
    private LocalDateTime createdAt;       // 创建时间
    private LocalDateTime updatedAt;       // 更新时间
}
