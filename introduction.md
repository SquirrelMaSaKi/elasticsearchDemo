# ElasticSearch
相当于是一个数据库，可以增删改查，但是提供了搜索功能的一个组件

我们的所有配置都放到了application.properties中，然后通过value注解，实现配置

#### 与一般数据库的名词转换

|ES|SQL|
|---|---|
|Index|database|
|Type|Table|
|Document是ES中的文档类型，一条数据对应一篇文档|一行数据row，ES中一个文档有多个字段也就是sql中一行有多个列|
|Field|列|
|mapping|schema|
|indexed 建立索引，默认是建立的|索引|
|QueryDSL json格式的查询语句|sql语句|
|GET/PUT/POST/DELETE|select/update/insert/delete|

#### 倒排索引
倒排索引就是，根据查询的内容查找索引，然后根据索引再去查所有相关内容进行返回

比如又如下的词库：华为{1,2}，Mate{1}，30{1}，荣耀{2,3}，9x{3}

当我们在搜索框输入【华为】的时候，我们会根据【1,2】索引将其他相关的Mate/30/荣耀都搜索除了，此外由于荣耀里有3，所以还有9x

并且，我们还可以进行高亮显示，比如将1这个索引找到的信息使用标签进行颜色高亮，这样华为、Mate和30这几个词就都高亮了

#### 分词器
传入一段内容，如何进行拆分，就是分词器的作用。

#### 需要制定规则mapping

es中的基本类型

字符型：string

string类型包括 text 和 keyword 

text类型被用来索引长文本，在建立索引前会将这些文本进行分词，转化为词的组合，建立索引允许es来检索这些 词语，text类型不能用来排序和聚合

Keyword类型**不**需要进行**分词**，可以被用来检索过滤、排序和聚合，keyword 类型字段只能用本身来进行检索 

数字型：long, integer, short, byte, double, float 

日期型：date 

布尔型：boolean 

二进制型：binary

#### bool过滤器
ES提供了bool过滤器查询

|关键字|作用|
|---|---|
|must|必须满足的条件，类似于sql中的and|
|should|可以满足也可以不满足的条件，类似于sql中的or|
|must_not|不需要满足的调节，类似于sql中的not|

我们的查询也有很多关键字

|关键字|作用|
|---|---|
|term和terms|**term query**会去倒排索引中寻找确切的值，它并不知道分词器的存在。这种查询适合keyword 、 numeric、date等不做分词的数据<br/>**terms**:查询某个字段里含有多个关键词的文档|
|from和size|控制查询返回数量，从哪一个文档开始，需要的个数|
|match|match query知道分词器的存在，会对field进行分词操作，然后查询，而使用term会查询不到结果<br/>match_all:查询所有文档<br/> multi_match:可以指定多个字段<br/> match_phrase:短语匹配查询|
|控制加载和返回指定字段|在query-match基础上使用，_source可以直接匹配字段返回指定字段，_source-includes/_source-excludes可以进行*通配符匹配|
|排序sort|在列名下使用asc和desc|
|match_phrase_prefix|前缀匹配查询|
|range|范围查询<br/> from,to,include_lower(是否包含左边界，默认true),include_upper(是否包含右边界，默认true),boost|
|fuzzy|模糊查询<br/> value--查询关键字<br/> boost--查询的权重值，默认是1.0<br/> min_similarity:设置匹配的最小相似度，默认值为0.5，对于字符串，取值为0-1(包括0和1);对于数值，取值可能大于1;对于日期型取值为1d,1m等，1d就代表1天<br/> prefix_length:指明区分词项的共同前缀长度，默认是0<br/> max_expansions:查询中的词项可以扩展的数目，默认可以无限大|
|highlight|高亮搜索结果，高亮的区域一般和查询的位置一致|

进行范围查询或者范围过滤的时候也有几个关键字

|标识|含义|
|---|---|
|gt|>|
|lt|<|
|gte|>=|
|lte|<=|

