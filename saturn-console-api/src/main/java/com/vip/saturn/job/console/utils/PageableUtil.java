package com.vip.saturn.job.console.utils;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableUtil {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 25;

    private PageableUtil() {

    }

    public static Pageable generatePageble(final int page, final int size) {
        return new Pageable() {
            @Override
            public int getPageNumber() {
                if (page <= 0) {
                    return DEFAULT_PAGE;
                }
                return page;
            }

            @Override
            public int getPageSize() {
                if (size <= 0) {
                    return DEFAULT_SIZE;
                }
                return size;
            }

            @Override
            public int getOffset() {
                return getPageSize() * (getPageNumber() - 1);
            }

            @Override
            public Sort getSort() {
                return null;
            }

            @Override
            public Pageable next() {
                return null;
            }

            @Override
            public Pageable previousOrFirst() {
                return null;
            }

            @Override
            public Pageable first() {
                return null;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }
        };
    }
}
