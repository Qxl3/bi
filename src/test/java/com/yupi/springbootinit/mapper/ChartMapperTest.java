package com.yupi.springbootinit.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class ChartMapperTest {
    @Resource
    private  ChartMapper chartMapper;

    @Test
    void query(){
        String sql="select * from chart";
        chartMapper.queryChartData(sql);
    }
}