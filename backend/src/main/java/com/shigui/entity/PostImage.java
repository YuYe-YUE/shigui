package com.shigui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** post_image 表，单据关联图片 */
@Data
@TableName("post_image")
public class PostImage {
    @TableId(type = IdType.AUTO)
    private Long id;                 // 自增主键
    private Long postId;             // 关联单据 ID
    private String imageUrl;         // 图片访问 URL
    private Integer sortOrder;       // 排序序号
    @TableLogic
    private Integer deleted;         // 逻辑删除标记
    private LocalDateTime createdAt; // 创建时间
}
