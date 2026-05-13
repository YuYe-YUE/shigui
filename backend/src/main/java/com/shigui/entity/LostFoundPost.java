package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lost_found_post")
public class LostFoundPost {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String postType;
    private String title;
    private String itemName;
    private String itemCategory;
    private String description;
    private String privateFeature;
    private String campusArea;
    private String locationName;
    private Double longitude;
    private Double latitude;
    private String storageLocation;
    private LocalDateTime eventTime;
    private String status;
    private LocalDateTime publishedAt;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
