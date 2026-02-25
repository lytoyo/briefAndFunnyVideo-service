package com.lytoyo.controller.server.search;

import com.lytoyo.common.domain.Blog;
import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.User;
import com.lytoyo.framework.aspectj.SysLog;
import com.lytoyo.service.BlogService;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Package:com.lytoyo.controller.server.search
 *
 * @ClassName:Search
 * @Create:2026/1/4 13:31
 **/

@RestController
@RequestMapping("/server/search")
public class SearchController {

    @Resource
    private BlogService blogService;

    @SysLog(value = "关键词补全",require = false,needLogin = false)
    @GetMapping("/complement")
    public Result keywordComplement(@RequestParam("keyword") String keyword){
        SearchHits<Blog> result = blogService.keywordComplement(keyword);
        return Result.success(result);
    }

    @SysLog(value = "关键字综合查询",require = true,needLogin = false)
    @GetMapping("/comprehensiveSearch")
    public Result comprehensiveSearch(@RequestParam("keyword")String keyword){
        Map<String,SearchHits> result = blogService.comprehensiveSearch(keyword);
        return Result.success(result);
    }

    @SysLog(value = "关键词帖子分页查询",require = true,needLogin = false)
    @GetMapping("/postQueries")
    public Result postQueries(@RequestParam("keyword")String keyword,@RequestParam("current")Integer current,
                          @RequestParam("size")Integer size){
        SearchHits<Blog> result = blogService.postQueries(keyword,current,size);
        return Result.success(result);
    }

    @SysLog(value = "关键词帖子分类分页查询",require = true,needLogin = false)
    @GetMapping("/categoryQueries")
    public Result categoryQueries(@RequestParam("keyword")String keyword,@RequestParam("type")String type,
                                  @RequestParam("current")Integer current,@RequestParam("size")Integer size){
        SearchHits<Blog> result = blogService.categoryQueries(keyword,type,current,size);
        return Result.success(result);
    }

    @SysLog(value = "关键词用户查询",require = true,needLogin = false)
    @GetMapping("/userQueries")
    public Result userQueries(@RequestParam("keyword")String keyword,@RequestParam("current")Integer current,
                              @RequestParam("size")Integer size){
        SearchHits<User> result = blogService.userQuries(keyword,current,size);
        return Result.success(result);
    }
}
