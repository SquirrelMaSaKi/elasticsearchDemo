package com.rj.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
//这个注解指明了库和表，这里充分发挥了一个类一张表
//@Document(indexName = "mydb9", type = "goods")
public class Goods implements Serializable {
    private Integer id;
    private String goodsname;
    private double price;
}
