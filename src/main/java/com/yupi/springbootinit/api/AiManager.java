package com.yupi.springbootinit.api;

import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import io.github.briqt.spark4j.model.response.SparkTextUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
@Slf4j
@Component
public class AiManager {
    public static String doChat(List<SparkMessage> messages){
        SparkClient sparkClient = new SparkClient();
        sparkClient.appid = "88d24ca4";
        sparkClient.apiKey = "bba7989969c316026647476527523fcc";
        sparkClient.apiSecret = "NWM0NjNiZjVlMjUyMzdhNDJmNDczZjI3";
        // 消息列表，可以在此列表添加历史对话记录
//        List<SparkMessage> messages = new ArrayList<>();
//        messages.add(SparkMessage.userContent("请你扮演我的语文老师李老师，问我讲解问题问题，希望你可以保证知识准确，逻辑严谨。"));
//        messages.add(SparkMessage.assistantContent("好的，这位同学，有什么问题需要李老师为你解答吗？"));
//        messages.add(SparkMessage.userContent("鲁迅和周树人小时候打过架吗？"));

        // 构造请求
        SparkRequest sparkRequest = SparkRequest.builder()
        // 消息列表
                .messages(messages)
        // 模型回答的tokens的最大长度,非必传，默认为2048。
        // V1.5取值为[1,4096]
        // V2.0取值为[1,8192]
        // V3.0取值为[1,8192]
                .maxTokens(2048)
        // 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
                .temperature(0.5)
        // 指定请求版本
                .apiVersion(SparkApiVersion.V3_0)
                .build();

        // 同步调用
        SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
        SparkTextUsage textUsage = chatResponse.getTextUsage();
        log.info("\n回答：" + chatResponse.getContent());
        log.info("\n提问tokens：" + textUsage.getPromptTokens()
                + "，回答tokens：" + textUsage.getCompletionTokens()
                + "，总消耗tokens：" + textUsage.getTotalTokens());
        String result=chatResponse.getContent();
        return result;
    }
}
