package com.shigui.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shigui.dto.AiMatchResult;
import com.shigui.dto.MatchResponse;
import com.shigui.dto.PostResponse;
import com.shigui.entity.LostFoundPost;
import com.shigui.entity.MatchRecord;
import com.shigui.mapper.MatchRecordMapper;
import com.shigui.service.AiMatchClient;
import com.shigui.service.LostFoundPostService;
import com.shigui.service.MatchRecordService;
import com.shigui.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatchRecordServiceImpl extends ServiceImpl<MatchRecordMapper, MatchRecord> implements MatchRecordService {
    private static final BigDecimal THRESHOLD = new BigDecimal("0.70");
    private static final int MAX_CANDIDATES = 20;
    private static final int MAX_RESULTS = 5;

    private final LostFoundPostService lostFoundPostService;
    private final AiMatchClient aiMatchClient;
    private final NotificationService notificationService;

    public MatchRecordServiceImpl(LostFoundPostService lostFoundPostService,
                                  AiMatchClient aiMatchClient,
                                  NotificationService notificationService) {
        this.lostFoundPostService = lostFoundPostService;
        this.aiMatchClient = aiMatchClient;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void generateMatchesForPost(Long postId) {
        LostFoundPost target = lostFoundPostService.getById(postId);
        if (target == null || !"MATCHING".equals(target.getStatus()) || Integer.valueOf(1).equals(target.getDeleted())) {
            return;
        }
        List<LostFoundPost> candidates = loadCandidates(target).stream()
                .sorted(Comparator.comparing(c -> ruleScore(target, c), Comparator.reverseOrder()))
                .limit(MAX_CANDIDATES)
                .toList();
        if (candidates.isEmpty()) return;
        AiMatchResult aiResult = callAi(target, candidates);
        Map<Long, LostFoundPost> candidateMap = candidates.stream().collect(Collectors.toMap(LostFoundPost::getId, c -> c));
        int created = 0;
        for (AiMatchResult.Decision decision : aiResult.getMatches()) {
            if (created >= MAX_RESULTS) break;
            LostFoundPost candidate = candidateMap.get(decision.getCandidatePostId());
            if (candidate == null || !Boolean.TRUE.equals(decision.getMatched())) continue;
            BigDecimal score = normalize(decision.getScore());
            if (score.compareTo(THRESHOLD) < 0) continue;
            if (createMatch(target, candidate, score, sanitize(decision.getReason()))) created++;
        }
    }

    @Override
    public Page<MatchResponse> listMine(Long userId, int page, int size) {
        LambdaQueryWrapper<MatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MatchRecord::getDeleted, 0);
        wrapper.orderByDesc(MatchRecord::getCreatedAt);
        Page<MatchRecord> entityPage = page(new Page<>(page, size), wrapper);
        List<MatchResponse> responses = entityPage.getRecords().stream()
                .map(r -> toResponse(r, userId))
                .filter(r -> r.getMyPost() != null)
                .toList();
        Page<MatchResponse> result = new Page<>(page, size);
        result.setRecords(responses);
        result.setTotal(responses.size());
        return result;
    }

    private List<LostFoundPost> loadCandidates(LostFoundPost target) {
        String oppositeType = "LOST".equals(target.getPostType()) ? "FOUND" : "LOST";
        return lostFoundPostService.list(new LambdaQueryWrapper<LostFoundPost>()
                .eq(LostFoundPost::getPostType, oppositeType)
                .eq(LostFoundPost::getStatus, "MATCHING")
                .eq(LostFoundPost::getDeleted, 0)
                .ne(LostFoundPost::getId, target.getId()));
    }

    private AiMatchResult callAi(LostFoundPost target, List<LostFoundPost> candidates) {
        try {
            return aiMatchClient.rankMatches(target, candidates);
        } catch (Exception e) {
            AiMatchResult fallback = new AiMatchResult();
            fallback.setMatches(candidates.stream().map(candidate -> {
                AiMatchResult.Decision d = new AiMatchResult.Decision();
                d.setCandidatePostId(candidate.getId());
                d.setMatched(true);
                d.setScore(ruleScore(target, candidate));
                d.setReason(ruleReason(target, candidate));
                return d;
            }).toList());
            return fallback;
        }
    }

    private boolean createMatch(LostFoundPost target, LostFoundPost candidate, BigDecimal score, String reason) {
        Long lostId = "LOST".equals(target.getPostType()) ? target.getId() : candidate.getId();
        Long foundId = "FOUND".equals(target.getPostType()) ? target.getId() : candidate.getId();
        Long existing = count(new LambdaQueryWrapper<MatchRecord>()
                .eq(MatchRecord::getLostPostId, lostId)
                .eq(MatchRecord::getFoundPostId, foundId));
        if (existing != null && existing > 0) return false;
        MatchRecord record = new MatchRecord();
        record.setLostPostId(lostId);
        record.setFoundPostId(foundId);
        record.setScore(score.setScale(4, RoundingMode.HALF_UP));
        record.setReason(reason);
        record.setDeleted(0);
        save(record);
        LostFoundPost lostPost = "LOST".equals(target.getPostType()) ? target : candidate;
        LostFoundPost foundPost = "FOUND".equals(target.getPostType()) ? target : candidate;
        notificationService.createMatchNotification(lostPost.getUserId(), record.getId(), lostPost.getItemName(), lostPost.getCampusArea(), score.toPlainString(), reason);
        notificationService.createMatchNotification(foundPost.getUserId(), record.getId(), foundPost.getItemName(), foundPost.getCampusArea(), score.toPlainString(), reason);
        return true;
    }

    private BigDecimal ruleScore(LostFoundPost target, LostFoundPost candidate) {
        BigDecimal score = BigDecimal.ZERO;
        if (equalsText(target.getCampusArea(), candidate.getCampusArea())) score = score.add(new BigDecimal("0.20"));
        if (equalsText(target.getItemCategory(), candidate.getItemCategory())) score = score.add(new BigDecimal("0.25"));
        score = score.add(timeScore(target, candidate));
        if (containsEither(target.getTitle() + target.getItemName(), candidate.getTitle() + candidate.getItemName())) score = score.add(new BigDecimal("0.20"));
        if (containsEither(target.getPrivateFeature(), candidate.getPrivateFeature())) score = score.add(new BigDecimal("0.15"));
        return normalize(score);
    }

    private BigDecimal timeScore(LostFoundPost target, LostFoundPost candidate) {
        if (target.getEventTime() == null || candidate.getEventTime() == null) return BigDecimal.ZERO;
        long days = Math.abs(Duration.between(target.getEventTime(), candidate.getEventTime()).toDays());
        if (days <= 1) return new BigDecimal("0.20");
        if (days <= 3) return new BigDecimal("0.12");
        if (days <= 7) return new BigDecimal("0.06");
        return BigDecimal.ZERO;
    }

    private MatchResponse toResponse(MatchRecord record, Long userId) {
        LostFoundPost lost = lostFoundPostService.getById(record.getLostPostId());
        LostFoundPost found = lostFoundPostService.getById(record.getFoundPostId());
        MatchResponse response = new MatchResponse();
        response.setId(record.getId());
        response.setScore(record.getScore());
        response.setReason(record.getReason());
        response.setCreatedAt(record.getCreatedAt());
        if (lost != null && lost.getUserId().equals(userId)) {
            response.setMyPost(toPostResponse(lost));
            response.setMatchedPost(toPostResponse(found));
        } else if (found != null && found.getUserId().equals(userId)) {
            response.setMyPost(toPostResponse(found));
            response.setMatchedPost(toPostResponse(lost));
        }
        return response;
    }

    private PostResponse toPostResponse(LostFoundPost post) {
        if (post == null) return null;
        PostResponse r = new PostResponse();
        r.setId(post.getId()); r.setPostType(post.getPostType()); r.setTitle(post.getTitle());
        r.setItemName(post.getItemName()); r.setItemCategory(post.getItemCategory());
        r.setDescription(post.getDescription()); r.setCampusArea(post.getCampusArea());
        r.setLocationName(post.getLocationName()); r.setStorageLocation(post.getStorageLocation());
        r.setEventTime(post.getEventTime()); r.setPublishedAt(post.getPublishedAt()); r.setStatus(post.getStatus());
        return r;
    }

    private String ruleReason(LostFoundPost target, LostFoundPost candidate) {
        return "系统根据校区、品类、时间和文本相似度判断为疑似匹配。";
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("\\d{3,}", "***");
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private boolean equalsText(String left, String right) {
        return left != null && right != null && left.trim().equals(right.trim());
    }

    private boolean containsEither(String left, String right) {
        if (left == null || right == null) return false;
        String a = left.replaceAll("\\s+", "");
        String b = right.replaceAll("\\s+", "");
        return !a.isBlank() && !b.isBlank() && (a.contains(b) || b.contains(a));
    }
}
