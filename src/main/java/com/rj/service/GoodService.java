package com.rj.service;


import com.rj.pojo.Goods;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public interface GoodService {
    //创建库表
    void creatIndex() throws IOException;

    //判断库是不是存在
    boolean existIndex(String indexName) throws IOException;

    //添加数据
    void add(Goods goods) throws IOException;

    //查询数据,因为可能会有很多条,所以是集合,每个的数据是json类型的key-vlaue数据,可以用map存放，有很多的map，所以再用List接收字符串即可
    List<Map> search(String params) throws IOException, ParseException;

    //查询数据相关数据条数
    long getCount(String params) throws ParseException, IOException;

    //删库
    void deleteIndex(String indexName) throws IOException;

    //更新数据
    String update(String params) throws IOException;

    //删除数据
    String deleteApi(String id) throws IOException;
}
