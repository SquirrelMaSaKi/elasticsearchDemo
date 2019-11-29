package com.rj.controller;

import com.rj.exception.ExceptionDict;
import com.rj.exception.SearchException;
import com.rj.pojo.Goods;
import com.rj.service.GoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goods")
public class GoodsController {
//    @Autowired
//    private GoodsRepository goodsRepository;

    @Autowired
    private GoodService goodService;

    //插入goods数据
    @RequestMapping("/add/{id}/{goodsname}/{price}")
    public String add(@PathVariable("id") Integer id, @PathVariable("goodsname") String goodsname, @PathVariable("price") double price) {
        try {
            Goods goods = new Goods();
            goods.setId(id);
            goods.setGoodsname(goodsname);
            goods.setPrice(price);
            goodService.add(goods);
//            goodsRepository.save(goods);
        } catch (Exception e) {
            e.printStackTrace();
            return "no";
        }
        return "ok";
    }


    /**
     * 搜索数据，我们制定了规则
     * 1.必须以键值对形式，以json字符串形式传参
     * 2.参数内容有：id商品编号、content搜索内容、price价格、pageNum当前页、pageSize一页数据量、highlightPreTag/highlightPostTag前缀和后缀高亮html标签
     */
    @RequestMapping("/search")
    public List<Map> search(@RequestParam("params") String params) {
        //检查是否传递了参数
        checkParam(params);

        try {
            List<Map> list = goodService.search(params);
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(); //没有就返回空集合
    }

    @RequestMapping("/searchCount")
    public long searchCount(@RequestParam("params") String params) {
        checkParam(params);
        try {
            long count = goodService.getCount(params);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @RequestMapping("/delete")
    public String delete(@RequestParam String indexName) {
        checkParam(indexName);
        try {
            goodService.deleteIndex(indexName);
            return "删除成功";
        } catch (IOException e) {
            e.printStackTrace();
            return "删除失败";
        }
    }

    @RequestMapping("/update")
    public String update(@RequestParam("params") String params) throws IOException {
        checkParam(params);
        return goodService.update(params);
    }

    @RequestMapping("/deleteType")
    public String deleteType(@RequestParam String id) {
        checkParam(id);
        try {
            return goodService.deleteApi(id);
        } catch (IOException e) {
            e.printStackTrace();
            return "失败";
        }
    }

    /**
     * 检查是否真的传递了参数
     */
    private void checkParam(String params) {
        if (params == null || params.trim().length()==0) {
            throw new SearchException(ExceptionDict.DEFAULT_ERROR_CODE, "参数没有传递");
        }
    }
}
