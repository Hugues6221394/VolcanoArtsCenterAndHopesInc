package com.volcanoartscenter.platform.shared.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CursorPage<T>(
        List<T> data,
        Pagination pagination
) {
    public record Pagination(
            String cursor,
            boolean hasNext,
            int pageSize,
            Long totalCount
    ) {}

    public static <T> CursorPage<T> of(List<T> items, String nextCursor, int pageSize, Long totalCount) {
        return new CursorPage<>(items, new Pagination(nextCursor, nextCursor != null, pageSize, totalCount));
    }
}
