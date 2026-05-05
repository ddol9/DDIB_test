package com.ddib.monolith.reservation.api.dto;

import java.util.List;
import java.util.function.Function;

public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {

    public static <T> CursorPageResponse<T> of(List<T> content, int size, Function<T, Long> cursorExtractor) {
        boolean hasNext = content.size() > size;
        List<T> result = hasNext ? content.subList(0, size) : content;
        Long nextCursor = result.isEmpty() ? null : cursorExtractor.apply(result.get(result.size() - 1));
        return new CursorPageResponse<>(result, nextCursor, hasNext);
    }
}
