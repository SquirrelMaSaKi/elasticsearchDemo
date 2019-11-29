package com.rj.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rj.pojo.Goods;
import com.rj.service.GoodService;
import com.rj.utils.SearchUtil;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("goodService")
public class GoodServiceImpl implements GoodService {
    @Autowired
    private ObjectMapper mapper; //jackjson，用于对象和Json字符串的处理
    @Autowired
    private RestHighLevelClient client; //获取client，会直接从ElasticSearchConfig中去获取client，因为已经通过@Bean注入到了工厂

    @Value("${elasticsearch.index.name}") //库名，会直接从application.properties中查找
    private String indexName;

    @Value("${elasticsearch.type.name}")
    private String typeName;

    @Override
    public void creatIndex() throws IOException {
        //判断是否存在库
        if (!existIndex(indexName)) {
            //如果不存在，就创建库索引，进行分片
            CreateIndexRequest request = new CreateIndexRequest(indexName);

            /**
             * 分片：number_of_shards表示有几个主机，es支持分布式
             * 从机：number_of_replicas表示每个主机有多少个从机
             * es的主不是完全独立的，而是互相关联的，比如第一个主机是1的主，是2的从，第二个主机是2的主，1的从，以此类推
             */
            request.settings(Settings.builder().put("number_of_replicas", 1).put("number_of_shards", 5).build());

            //利用searchutil工具类映射创建表
            SearchUtil.buildMapping(typeName, request);

            //返回结果
            CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);

        } else {
            System.err.println("创建失败,库已经存在");
        }
    }

    @Override
    public boolean existIndex(String indexName) throws IOException {
        //获取数据库索引请求
        GetIndexRequest request = new GetIndexRequest();

        //指定要查询哪个库
        request.indices(indexName);

        //判断返回结果，利用client中的地址访问到对应的es，然后根据库索引，导入索引请求和默认处理方式进行存在性判断
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    @Override
    public void add(Goods goods) throws IOException {
        /**
         * 创建索引需求，指明库和表
         * 这里注意，IndexRequest构造需要传入三个参数index库名、tape表名和id数据索引
         * 这个数据索引如果要自定义则必须进行修改，否则影响修改删除操作
         * 如果不自定义，则系统会自动生成UUID
         */
        IndexRequest request = new IndexRequest(indexName, typeName, goods.getId().toString());

        //将对象传入，注意，必须是json格式
        request.source(mapper.writeValueAsString(goods), XContentType.JSON);
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);

        //返回插入结果
//        System.err.println("插入的数据结果是："+mapper.writeValueAsString(response));
    }

    @Override
    public List<Map> search(String params) throws IOException, ParseException {
        //创建list集合
        List list = new ArrayList();

        //用户通过ES传过来的参数也是json字符串，需要进行解析，然后调用SearchUtil工具类获取builder
        Map map = mapper.readValue(params, Map.class);
        SearchSourceBuilder searchSourceBuilder = SearchUtil.getSearchSourceBuilder(map);

        //这里需要进行一下高亮的替换，我们通过工具，确立了高亮规则，并将相关的【词+前后标签】组合结果存储，这里需要替换掉原始数据
        //创建SearchRequest，指定库表，然后将规则导入
        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(searchSourceBuilder);

        //获取返回值
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        //得到命中数据，转为可操作的数据进行遍历
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit searchHit : searchHits) {
            String source = searchHit.getSourceAsString(); //原始数据
            Map sourceMap = mapper.readValue(source, Map.class); //json字符串解析为map对象

            //我们通过数组拿到的结果是json字符串，需要进行解析为map对象，前端传递的是键值对对象
            //获取高亮数据，按照规则，其实就是content参数的值，然后替换掉sourceMap中的数据
            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
            HighlightField highlightContent = highlightFields.get("goodsname");

            //判断，不为空，说明有高亮，要进行处理
            if (highlightContent != null) {
                Text[] texts = highlightContent.fragments();
                if (texts != null && texts.length != 0) {
                    String hlcontent = texts[0].string(); //确实获取了高亮内容，是没有被分词器进行处理过的
                    sourceMap.put("goodsname", hlcontent); //替换
                }
            }

            //list集合将所有数据接收
            list.add(sourceMap);
        }
        return list;
    }

    @Override
    public long getCount(String params) throws ParseException, IOException {
        Map map = mapper.readValue(params, Map.class);
        SearchSourceBuilder searchSourceBuilder = SearchUtil.getSearchSourceBuilder(map);
        SearchRequest request = new SearchRequest(indexName);
        request.source(searchSourceBuilder);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        return response.getHits().totalHits;
    }

    @Override
    public void deleteIndex(String indexName) throws IOException {
        if (existIndex(indexName)) {
            //创建DeleteIndexRequest
            DeleteIndexRequest request = new DeleteIndexRequest(indexName);

            //反馈response，你会发现，凡是和库表创建删除相关操作，返回结果都是indices()方法
            DeleteIndexResponse response = client.indices().delete(request, RequestOptions.DEFAULT);
            System.err.println("删除结果是：" + mapper.writeValueAsString(response));
        }
    }

    @Override
    public String update(String params) throws IOException {
        Map map = mapper.readValue(params, Map.class);

        String id = (String) map.get("id");

        //更新需要传入index/type/id，这个id是索引id，我们这里定义为数据的id
        UpdateRequest updateRequest = new UpdateRequest(indexName,typeName,id).doc(map);
        client.update(updateRequest, RequestOptions.DEFAULT);
        return null;
    }

    @Override
    public String deleteApi(String id) throws IOException {
        if (id != null && id.trim().length() != 0) {
            DeleteRequest deleteRequest = new DeleteRequest(indexName, typeName, id);
            client.delete(deleteRequest, RequestOptions.DEFAULT);
            return "删除成功";
        }
        return "删除失败";
    }
}
