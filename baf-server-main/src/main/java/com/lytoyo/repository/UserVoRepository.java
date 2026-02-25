package com.lytoyo.repository;

import com.lytoyo.common.domain.vo.UserVo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Package:com.lytoyo.repository
 *
 * @ClassName:UserVoRepository
 * @Create:2026/1/5 9:09
 **/
public interface UserVoRepository extends ElasticsearchRepository<UserVo,Long> {
}
