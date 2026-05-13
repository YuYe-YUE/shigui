package com.shigui.service;

import com.shigui.dto.CreatePostRequest;
import com.shigui.dto.PostResponse;
import com.shigui.entity.AppUser;
import com.shigui.entity.LostFoundPost;
import com.shigui.mapper.LostFoundPostMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LostFoundPostServiceTest {

    @Test
    void compileContract_lostFoundPostAndDtosExposeSprint2Fields() {
        CreatePostRequest request = new CreatePostRequest();
        request.setPostType("LOST");
        request.setTitle("丢失校园卡");
        request.setItemName("校园卡");
        request.setItemCategory("证件");
        request.setDescription("绿色卡套");
        request.setPrivateFeature("卡号后四位 1234");
        request.setCampusArea("南校园");
        request.setLocationName("逸夫楼");
        request.setLongitude(113.2931234);
        request.setLatitude(23.0961234);
        request.setStorageLocation("");
        request.setEventTime(LocalDateTime.of(2026, 5, 13, 9, 30));

        LostFoundPost post = new LostFoundPost();
        post.setPostType(request.getPostType());
        post.setTitle(request.getTitle());

        PostResponse response = new PostResponse();
        response.setId(1L);
        response.setStatus("PENDING_AUDIT");

        assertThat(post.getPostType()).isEqualTo("LOST");
        assertThat(response.getStatus()).isEqualTo("PENDING_AUDIT");
        assertThat(LostFoundPostMapper.class).isInterface();
        assertThat(AppUser.class).isNotNull();
    }
}
