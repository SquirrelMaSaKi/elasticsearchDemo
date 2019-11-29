package com.rj;

import com.rj.pojo.Goods;
import com.rj.service.GoodService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MyelasticsearchApplication.class)
@WebAppConfiguration
public class ElasticSearchTest {
    @Autowired
    private GoodService goodService;

    @Test
    public void testCreateIndex() throws IOException {
        goodService.creatIndex();
    }

    @Test
    public void testAdd() throws IOException {
        Goods goods = new Goods();
        goods.setId(1);
        goods.setGoodsname("苹果");
        goods.setPrice(30000);
        goodService.add(goods);
    }
}
