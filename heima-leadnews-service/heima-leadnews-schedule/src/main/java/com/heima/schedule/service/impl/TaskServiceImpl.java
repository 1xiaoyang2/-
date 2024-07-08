package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.common.constans.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@Transactional
public class TaskServiceImpl implements TaskService {
    @Autowired
    private TaskinfoMapper taskinfoMapper;
    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;
    @Autowired
    private CacheService cacheService;

    @Override
    public boolean cancelTask(Long taskId) {
        //删除数据库中的任务以及更新日志表
        Task task = updateDb(taskId,ScheduleConstants.CANCELLED);

        boolean flag = false;
        //删除redis中数据
        if (task==null){
            deleteTaskFromCache(task);
            flag = true;
        }
        return flag;
    }

    private void deleteTaskFromCache(Task task) {
        //
        String key = task.getTaskType() + "_" + task.getPriority();
        if(task.getExecuteTime() <= System.currentTimeMillis()){
            cacheService.lRemove(ScheduleConstants.TOPIC+key,0,JSON.toJSONString(task));
        }else {
            cacheService.zRemove(ScheduleConstants.FUTURE+key,JSON.toJSONString(task));
        }
    }

    private Task updateDb(Long taskId, int status) {
        Task task = null;
        try {
            //根据id删除任务
            taskinfoMapper.deleteById(taskId);
            //更新日志
            TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
            taskinfoLogs.setStatus(status);
            taskinfoLogsMapper.updateById(taskinfoLogs);

            //返回task
            task = new Task();
            BeanUtils.copyProperties(taskinfoLogs,task);
            task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());
        } catch (BeansException e) {
            log.error("任务取消异常 taskId:{}",taskId);
        }
        return task;
    }

    @Override
    public long addTask(Task task) {
        //添加任务到数据库
        boolean success = addTaskToDb(task);

        if(success){
            //添加任务到缓存
            addTaskToCache(task);
        }
        return 0;
    }

    /**
     * @Description: 添加任务到缓存
     * @param task
     */
    private void addTaskToCache(Task task) {
        String key = task.getTaskType() + "_" + task.getPriority();

        //获取5分钟后的毫秒值
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE,5);
        long timeInMillis = instance.getTimeInMillis();

        if(task.getExecuteTime() <= System.currentTimeMillis()){
            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));
        } else if (task.getExecuteTime() <= timeInMillis) {
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), task.getExecuteTime());
        }

    }


    /**
     * @Description: 添加任务到数据库
     * @param task
     * @return: boolean
     */
    private boolean addTaskToDb(Task task) {
        boolean flag = false;
        try {
            //保存任务表
            Taskinfo taskinfo =new Taskinfo();
            BeanUtils.copyProperties(task,taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);

            task.setTaskId(taskinfo.getTaskId());

            //保存日志表
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(taskinfo,taskinfoLogs);
            taskinfoLogs.setVersion(1);
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            taskinfoLogsMapper.insert(taskinfoLogs);

            flag = true;
        } catch (Exception e) {
            e.getStackTrace();
        }
        return flag;
    }

    /**
     * @Description: 消费任务
     * @param type
     * @param priority
     * @return: com.heima.model.schedule.dtos.Task
     */
    @Override
    public Task poll(int type, int priority) {
        Task task = null;

        try {
            String key = type + "_" + priority;
            String jsonObject = cacheService.lRightPop(ScheduleConstants.TOPIC + key);
            if (jsonObject!=null) {
                task = JSON.parseObject(jsonObject, Task.class);
                updateDb(task.getTaskId(), ScheduleConstants.EXECUTED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return task;
    }


    /**
     * @Description: 定时刷新
     * @param
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void refresh() {
        //获取keys
        Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
        for (String futureKey : futureKeys) {
            String topicKey = ScheduleConstants.TOPIC + futureKey.split(ScheduleConstants.FUTURE)[1];
            Set<String> tasks = cacheService.zRangeByScore(futureKey, 0, System.currentTimeMillis());

            //同步数据
            if(!tasks.isEmpty()){
                cacheService.refreshWithPipeline(futureKey, topicKey, tasks);
            }
        }
    }


    /**
     * @Description: 数据库同步到redis中
     * @param
     */
    @PostConstruct
    @Scheduled(cron = "0 */5 * * * ?")
    public void reloadData(){
        //删除redis中的任务
        Set<String> topicKeys = cacheService.scan(ScheduleConstants.TOPIC + "*");
        Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
        cacheService.delete(topicKeys);
        cacheService.delete(futureKeys);

        //将数据库中的任务添加到redis中
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE,5);
        long timeInMillis = instance.getTimeInMillis();
        List<Taskinfo> taskinfos = taskinfoMapper
                .selectList(new LambdaQueryWrapper<Taskinfo>().lt(Taskinfo::getExecuteTime, timeInMillis));

        if(!CollectionUtils.isEmpty(taskinfos)){
            for (Taskinfo taskinfo : taskinfos) {
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo, task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                addTaskToCache(task);
            }
        }
    }
}
