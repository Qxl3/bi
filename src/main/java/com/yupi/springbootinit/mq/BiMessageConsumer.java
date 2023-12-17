package com.yupi.springbootinit.mq;


import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.api.AiManager;
import com.yupi.springbootinit.common.ChartStatus;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.BiMqConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import io.github.briqt.spark4j.model.SparkMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;

@Component
@Slf4j
public class BiMessageConsumer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;



    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME},ackMode="MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliverTag)  {
        log.info("receiveMessage message={}",message);
        if(StringUtils.isBlank(message)){
            channel.basicNack(deliverTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        long chartId= Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if(chart==null){
            channel.basicNack(deliverTag,false,false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"图表为空");
        }
        //先修改任务执行状态为执行中
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(ChartStatus.running.toString());
        boolean updateResultOne = chartService.updateById(updateChart);
        if(!updateResultOne){
            channel.basicNack(deliverTag,false,false);
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"数据库出现异常，更新失败！");
        }
        //调用AI处理数据
        String result = AiManager.doChat(buildUserInput(chart));
        //处理AI响应回答
        String[] splits = result.split("【|】|```json|```");
        //判断AI响应结果是否有效
        if(splits.length!=7){
            channel.basicNack(deliverTag,false,false);
            handlerChartUpdateError(chart.getId(),"AI生成错误");
            return;
        }
        String genChart=splits[3].trim();
        String genResult=splits[6].trim();
        //AI执行成功，修改任务执行状态
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setStatus(ChartStatus.succeed.toString());
        updateChartResult.setGenResult(genResult);
        updateChartResult.setGenChart(genChart);
        boolean updateResultTwo = chartService.updateById(updateChartResult);
        if(!updateResultTwo){
            channel.basicNack(deliverTag,false,false);
            handlerChartUpdateError(chart.getId(),"更新图表成功状态失败");
            return;
        }
        //任务成功，手动确认消息成功
        channel.basicAck(deliverTag,false);
    }




    //AI处理失败调用
    private void handlerChartUpdateError(long chartId,String execMessage){
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(ChartStatus.failed.toString());
        updateChart.setExecMessage(execMessage);
        boolean updateResult =chartService.updateById(updateChart);
        if(!updateResult) {
            log.error("更新图表失败状态失败");
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"数据库出现异常，更新失败！");
        }
    }

    private ArrayList<SparkMessage> buildUserInput(Chart chart){
        String goal=chart.getGoal();
        String chartType=chart.getChartType();
        String csvData=chart.getChartData();

        //拼接
        String compostData= compostUserInput(goal, csvData,chartType);
        ArrayList<SparkMessage> sparkMessage = new ArrayList<>();
        sparkMessage.add(SparkMessage.userContent(compostData));
        return sparkMessage;
    }


    public String compostUserInput(String goal,String data,String type){
        StringBuilder result=new StringBuilder();
        result.append("现在假设你是一个数据分析师和前端开发专家，下面我会提供给你一个分析需求和一些原始数据和图表类型，需要你完成两个操作，第一个操作是根据我给你的原始数据，然后按照我指定的图标类型来输出前端 Echarts V5 的 option 配置对象json代码，第二个操作是根据我提供给你的原始数据并按照我的分析需求来给出一个明确的数据分析结论，输出的格式为：\n" +
                        "【前端 Echarts V5 的 option 配置对象json代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释】\n" +
                        "【明确的数据分析结论、越详细越好，不要生成多余的注释】")
                .append("下面是我给你的相关参数："+'\n')
                .append("分析需求："+goal)
                .append("原始数据："+"\n"+data)
                .append("图表类型："+type);
        return result.toString();
    }
}
