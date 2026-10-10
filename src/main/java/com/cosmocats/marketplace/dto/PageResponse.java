package com.cosmocats.marketplace.dto;

import java.util.List;

/** Paged envelope returned by list endpoints. {@code page} is zero-based. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements) {
}
