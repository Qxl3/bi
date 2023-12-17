package com.yupi.springbootinit.controller;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.api.AiManager;
import com.yupi.springbootinit.common.*;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.mq.BiMessageProducer;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import com.yupi.springbootinit.utils.SqlUtils;
import io.github.briqt.spark4j.model.SparkMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 帖子接口
 *
 
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;


    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);

        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }




    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */

    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        String name =chartQueryRequest.getName();
        Long id = chartQueryRequest.getId();
        Long userId = chartQueryRequest.getUserId();


        queryWrapper.eq(id!=null&&id>0,"id",id);
        queryWrapper.like(StringUtils.isNotBlank(name),"name",name);
        queryWrapper.eq(StringUtils.isNotBlank(goal),"goal",goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType),"chartType",chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String type = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
        User loginUser = userService.getLoginUser(request);
        //转换用户输入表格数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        //拼接
        String compostData= compostUserInput(goal, csvData,type);
        ArrayList<SparkMessage> sparkMessages = new ArrayList<>();
        sparkMessages.add(SparkMessage.userContent(compostData));
        //调用AI处理数据
        String result = AiManager.doChat(sparkMessages);
        //处理AI响应回答
        String[] splits = result.split("【|】|```json|```");
        //判断AI响应结果是否有效
        if(splits.length!=7){
            return ResultUtils.error( ErrorCode.SYSTEM_ERROR,"AI生成错误");
        }
        String genChart=splits[3].trim();
        String genResult=splits[6].trim();
        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(type);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setStatus(ChartStatus.succeed.toString());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }



    /**
     * 智能分析（异步_线程池）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest,HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String type = genChartByAiRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length()>100,ErrorCode.PARAMS_ERROR,"名称过长");
        //校验文件大小
        long size = multipartFile.getSize();
        final long ONE_MB=1024*1024L;
        ThrowUtils.throwIf(size>ONE_MB,ErrorCode.PARAMS_ERROR,"文件超过1M");
        //校验文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);//获取文件名后缀
        final List<String> validFileSuffix= Arrays.asList("xlsx");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");
        User loginUser = userService.getLoginUser(request);
        //转换用户输入表格数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        //拼接
        String compostData= compostUserInput(goal, csvData,type);
        ArrayList<SparkMessage> sparkMessages = new ArrayList<>();
        sparkMessages.add(SparkMessage.userContent(compostData));


        //先将用户的图标请求（为处理过的）插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(type);
        chart.setStatus(ChartStatus.wait.toString());
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表数据保存错误");

        //异步化提交任务，后台调用线程去处理这个任务
        //todo 建议处理任务队列满后抛异常的情况
        //todo 限流
        CompletableFuture.runAsync(()->{
            //先修改任务执行状态为执行中
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(ChartStatus.running.toString());
            boolean updateResultOne = chartService.updateById(updateChart);
            if(!updateResultOne){
                throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"数据库出现异常，更新失败！");
            }
            //调用AI处理数据
            String result = AiManager.doChat(sparkMessages);
            //处理AI响应回答
            String[] splits = result.split("【|】|```json|```");
            //判断AI响应结果是否有效
            if(splits.length!=7){
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
                handlerChartUpdateError(chart.getId(),"更新图表成功状态失败");
                return;
            }
        },threadPoolExecutor);

        //先返回前端未处理过的数据
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);

    }


    /**
     * 智能分析（异步_消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest,HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String type = genChartByAiRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length()>100,ErrorCode.PARAMS_ERROR,"名称过长");
        //校验文件大小
        long size = multipartFile.getSize();
        final long ONE_MB=1024*1024L;
        ThrowUtils.throwIf(size>ONE_MB,ErrorCode.PARAMS_ERROR,"文件超过1M");
        //校验文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);//获取文件名后缀
        final List<String> validFileSuffix= Arrays.asList("xlsx");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");
        User loginUser = userService.getLoginUser(request);
        //转换用户输入表格数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        //拼接
        String compostData= compostUserInput(goal, csvData,type);
        ArrayList<SparkMessage> sparkMessages = new ArrayList<>();
        sparkMessages.add(SparkMessage.userContent(compostData));


        //先将用户的图标请求（为处理过的）插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(type);
        chart.setStatus(ChartStatus.wait.toString());
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表数据保存错误");
        Long newChartId=chart.getId();
        //提交到消息队列
        biMessageProducer.sendMessage(String.valueOf(newChartId));

        //先返回前端未处理过的数据
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(newChartId);
        return ResultUtils.success(biResponse);

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
