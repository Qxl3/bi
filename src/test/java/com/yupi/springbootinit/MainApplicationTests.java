package com.yupi.springbootinit;

import com.yupi.springbootinit.api.AiManager;
import io.github.briqt.spark4j.model.SparkMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.ArrayList;



@SpringBootTest
class MainApplicationTests {


    @Test
    void contextLoads() {
        ArrayList<SparkMessage> message = new ArrayList<>();
        message.add(SparkMessage.userContent("你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\\n\" +\n" +
                "                \"分析需求：\\n\" +\n" +
                "                \"帮我分析一下网站数据\\n\" +\n" +
                "                \"原始数据：\\n\" +\n" +
                "                \"日期,浏览人数\\n\" +\n" +
                "                \"1,10\\n\" +\n" +
                "                \"2,20\\n\" +\n" +
                "                \"3,30\\n\" +\n" +
                "                \"请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\\n\" +\n" +
                "                \"【【【【【\\n\" +\n" +
                "                \"{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\\n\" +\n" +
                "                \"【【【【【\\n\" +\n" +
                "                \"{明确的数据分析结论、越详细越好，不要生成多余的注释}\""));
        AiManager.doChat(message);

    }

}
