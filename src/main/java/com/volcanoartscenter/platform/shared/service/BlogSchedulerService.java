package com.volcanoartscenter.platform.shared.service;

import com.volcanoartscenter.platform.shared.model.BlogPost;
import com.volcanoartscenter.platform.shared.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Auto-publishes blog posts that have a scheduled publishedAt in the past.
 * Runs every minute as specified in the Master Backend Spec (Section 6 — Blog & CMS).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogSchedulerService {

    private final BlogPostRepository blogPostRepository;

    @Scheduled(fixedRate = 60_000) // every 60 seconds
    @Transactional
    public void publishScheduledPosts() {
        LocalDateTime now = LocalDateTime.now();
        List<BlogPost> scheduledPosts = blogPostRepository
                .findByPublishedFalseAndPublishedAtNotNullAndPublishedAtBefore(now);

        for (BlogPost post : scheduledPosts) {
            post.setPublished(true);
            blogPostRepository.save(post);
            log.info("Auto-published blog post: '{}' (id={})", post.getTitle(), post.getId());
        }

        if (!scheduledPosts.isEmpty()) {
            log.info("BlogScheduler: published {} scheduled post(s).", scheduledPosts.size());
        }
    }
}
