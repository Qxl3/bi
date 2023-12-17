package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;

import java.util.List;
import java.util.Map;

/**
* @author ASUS
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2023-11-22 19:55:57
* @Entity com.yupi.springbootinit.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {

   List<Map<String,Object>> queryChartData(String querySql);
}




