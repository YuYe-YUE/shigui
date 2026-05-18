package com.shigui.dto;
import lombok.Data;
@Data
public class ChatSessionResponse {
    private Long id; private Long postId;
    private String currentUserRole; private String peerRole;
    private String status;
}
