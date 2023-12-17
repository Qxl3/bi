package com.yupi.springbootinit.model.dto.chart;

import lombok.Data;

@Data
public class GenChartByAiRequest {
    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;
}
