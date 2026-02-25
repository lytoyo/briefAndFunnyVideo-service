package com.lytoyo.message;

import com.lytoyo.common.domain.Blog;
import com.lytoyo.repository.BlogRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.channels.Channel;

/**
 * Package:com.lytoyo.message
 *
 * @ClassName:BlogMessageLister
 * @Create:2025/12/29 17:25
 **/
@Component
public class BlogMessageLister {

    @Resource
    private BlogRepository blogRepository;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true", value = "queue.baf.up.blog"),
            exchange = @Exchange(value = "exchange.direct.baf"), key = {"routing.baf.up.blog"}))
    public void blogUpElasticsearchProcessMessage(Blog blog) {
        blogRepository.save(blog);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true", value = "queue.baf.del.blog"),
            exchange = @Exchange(value = "exchange.direct.baf"), key = {"routing.baf.del.blog"}))
    public void blogDelElasticsearchProcessMessage(Blog blog) {
        blogRepository.delete(blog);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true", value = "queue.baf.mod.blog"),
            exchange = @Exchange(value = "exchange.direct.baf"), key = {"routing.baf.mod.blog"}))
    public void blogModElasticsearchProcessMessage(Blog blog) {
        blogRepository.deleteById(blog.getId());
        blogRepository.save(blog);
    }
}
