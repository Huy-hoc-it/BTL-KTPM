package com.example.cinema.modules.movie.api;

public class PaginationMeta {
    private int page;
    private int pageSize;
    private long totalItems;
    private long totalPages;

    public PaginationMeta(int page, int pageSize, long totalItems, long totalPages) {
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public long getTotalPages() {
        return totalPages;
    }
}
