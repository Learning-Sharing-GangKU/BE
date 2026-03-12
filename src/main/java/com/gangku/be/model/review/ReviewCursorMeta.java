package com.gangku.be.model.review;

public record ReviewCursorMeta(int size, String sortedBy, String nextCursor, boolean hasNext) {
    public static ReviewCursorMeta of(
            int size, String sortedBy, String nextCursor, boolean hasNext) {
        return new ReviewCursorMeta(size, sortedBy, nextCursor, hasNext);
    }
}
