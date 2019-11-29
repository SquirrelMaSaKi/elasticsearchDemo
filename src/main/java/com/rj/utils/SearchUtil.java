package com.rj.utils;


import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

/**
 * 这里有一个小纰漏：
 * MatchQueryBuilder、TermQueryBuilder，无论哪种Builder，对应要操作的对象必须都是goods属性，【同名】
 */
public class SearchUtil {
    public static void buildMapping(String typeName, CreateIndexRequest request) throws IOException {
        /**
         * 以下操作目的就是创建表，并将表的属性设置好，相当于是sql中的create table(属性...);
         * 表的属性，依靠startObject()和endObject()进行分隔（相当于是列名），首先是表的分隔，然后是一个个属性的分隔
         * field就是该列的具体类型，integer/string...
         */
        XContentBuilder builder = JsonXContent.contentBuilder().startObject()
                .startObject("properties") //必须有properties，这是映射标识
                .startObject("id")
                .field("type", "keyword")//列类型
                .field("index","true")//是否设置索引，主要用来倒排索引用
                .endObject()
                .startObject("goodsname")
                .field("type","text")//string在es中就是text或keyword
                .field("index","true")
                .endObject()
                .startObject("price")
                .field("type", "long")
                .field("index", "true")
                .endObject()
                .endObject()
        .endObject();
        //返回结果
        request.mapping(typeName, builder);
    }

    /**
     * 我们在这里制定的是【规则】，即按照什么标准进行查询，用户应该向我们输入什么参数等等
     * 我们会在service层将我们的【规则】给到es，通过SearchRequest去匹配对应的库，通过source方法导入规则查询出结果
     * 根据用户关键字进行查询，返回的是builder
     */
    public static SearchSourceBuilder getSearchSourceBuilder(Map map) throws ParseException {
        /**
         * 一、查询过滤BoolQueryBuilder的规则设定
         * 用户传过来的查询条件会有很多，这意味着，匹配是不一样的
         * 比如用户传过来的上架时间和下架时间，我们可以使用range进行查询
         * 比如商品的id我们可以使用term查询
         * 请求的正文，我们可以使用match查询
         * 我们应当能够满足所有的条件，所以我们可以使用bool过滤器查询
         */
        //创建SearchSourceBuilder
        //创建bool过滤器查询
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //利用map获取到传过来的查询条件，并且创建出查询类别，然后根据条件是否为空，将调节值放入到查询类型，同时给到bool
        Object id = map.get("id");
        Object goodsname = map.get("goodsname");
        Object price = map.get("price");
        TermQueryBuilder termId = null;
        TermQueryBuilder termprice = null;
        MatchQueryBuilder matchGoodsname = null; //专门处理goodsname，任何搜索内容

        //声明分页
        String pageNum = (String) map.get("pageNum");
        String pageSize = (String) map.get("pageSize");
        int pn;
        int ps;

        //以下条件是可以联查的，联查方式取决于must/should/must_not
        if (id != null) {
            termId = new TermQueryBuilder("id", id);
            boolQueryBuilder.must(termId);
        }

        if (goodsname != null) {
            matchGoodsname = new MatchQueryBuilder("goodsname",goodsname);
            boolQueryBuilder.must(matchGoodsname);
        }

        if (price != null) {
            termprice = new TermQueryBuilder("price", price);
            boolQueryBuilder.should(termprice);
        }
        /**
         * 以下为时间范围的查询，仅供参考
         * 会有三种情况，用户只查询开始时间，或者只查询结束时间，或者两者都查询
         */
//        public SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//        Object startTime = map.get("startTime");
//        Object endTime = map.get("endTime");
//        RangeQueryBuilder receiveTimeQuery = null;
//        if (startTime != null && endTime != null) {
//            Date start = simpleDateFormat.parse(startTime.toString());
//            Date end = simpleDateFormat.parse(endTime.toString());
//            receiveTimeQuery = QueryBuilders.rangeQuery("startTime").gte(start.getTime()).lte(end.getTime());
//            boolQueryBuilder.must(receiveTimeQuery);
//        } else if (startTime == null && endTime != null) {
//            Date end = simpleDateFormat.parse(endTime.toString());
//            receiveTimeQuery = QueryBuilders.rangeQuery("startTime").lte(end.getTime());
//            boolQueryBuilder.must(receiveTimeQuery);
//        } else if (startTime != null && endTime == null) {
//            Date start = simpleDateFormat.parse(startTime.toString());
//            receiveTimeQuery = QueryBuilders.rangeQuery("startTime").gte(start.getTime());
//            boolQueryBuilder.must(receiveTimeQuery);
//        }
        //最后将bool给到SearchSourceBuilder
        searchSourceBuilder.query(boolQueryBuilder);

        /**
         * 分页处理规则
         * 分页,from和size,相当于是limit 对应的两个值，第一个是偏移量，第二个是每页数据量，这里需要传递两个值pageNum和pageSize
         */
        if (pageNum != null && pageNum.trim().length()!=0) {
            pn = Integer.valueOf(pageNum);
            if (pn < 1) {
                pn = 1;
            }
        } else {
            pn = 1;
        }

        if (pageSize != null && pageSize.trim().length()!=0) {
            ps = Integer.valueOf(pageSize);
            if (ps < 1) {
                ps = 5;
            }
        } else {
            ps = 5;
        }
        //导入serarchSourceBuilder
        searchSourceBuilder.from((pn-1)*ps).size(ps);

        /**
         * 高亮规则
         * 高亮，应该要设置高亮数据,理论上高亮的标签应该是用户传递的,当然我们也有默认的
         * 我们这里只针对goodsname数据
         */
        //判断，只有传递了内容goodsname才进行高亮处理
        if (goodsname != null) {
            //我们从参数中获取前后缀参数的值，用户不传递，我们就自定义
            String highlightPreTag = (String) map.get("highlightPreTag");
            String highlightPostTag = (String) map.get("highlightPostTag");

            //获取高亮的前缀和后缀，判断，如果为空，我们就为其增加html色彩标签
            if (highlightPreTag == null || highlightPreTag.trim().length() == 0) {
                highlightPreTag = "<span color='red'>";
            }
            if (highlightPostTag == null || highlightPostTag.trim().length() == 0) {
                highlightPostTag = "</span>";
            }

            //创建高亮HighlightBuilderBuilder，类似于BoolQueryBuilder，通过field将goodsname导入，并设置前后缀
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("goodsname"); //将结果放入到goods的goodsname属性
            highlightBuilder.preTags(highlightPreTag);
            highlightBuilder.postTags(highlightPostTag);

            //导入serarchSourceBuilder
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        return searchSourceBuilder;
    }
}
