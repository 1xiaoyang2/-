package com.heima.schedule.service;

import com.heima.model.schedule.dtos.Task;

/**
 * 对外访问接口
 */
public interface TaskService {

    /**
     * 添加任务
     * @param task   任务对象
     * @return       任务id
     */
    public long addTask(Task task) ;

    /**
     * 删除任务
     * @param taskId   任务对象
     * @return       任务id
     */
    public boolean cancelTask(Long taskId) ;

    /**
     * @Description: 消费任务
     * @param type
     * @param priority
     * @return: com.heima.model.schedule.dtos.Task
     */
    public Task poll(int type, int priority);
}