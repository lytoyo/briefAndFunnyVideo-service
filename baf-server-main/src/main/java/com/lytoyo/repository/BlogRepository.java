package com.lytoyo.repository;

import com.lytoyo.common.domain.Blog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * Package:com.lytoyo.common.repository
 *
 * @ClassName:BlogRepository
 * @Create:2025/12/29 17:33
 **/

public interface BlogRepository extends ElasticsearchRepository<Blog,Long> {

}
