package com.dangdang.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepositoryCustom {

    Page<Item> search(ItemSearchCondition condition, Pageable pageable);
}
