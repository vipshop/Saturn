package com.vip.saturn.job.console.utils;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableUtil {
    public static Pageable generatePageble(final int page, final int size) {
        return new Pageable() {
            @Override
            public int getPageNumber() {
                return page;
            }

            @Override
            public int getPageSize() {
                return size;
            }

            @Override
            public int getOffset() {
                return size * (page - 1);
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
