package com.secretbase.Job;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class JobScheduler {

    public static void start() throws SchedulerException {
        // 1、创建调度器Scheduler
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();
        // 2、创建JobDetail实例，并与PrintWordsJob类绑定(Job执行内容)
        JobDetail jobDetail = JobBuilder.newJob(SyncToMySqlJob.class)
                .withIdentity("job1", "group1").build();
        // 3、构建Trigger实例,每隔1s执行一次

        CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity("trigger1", "triggerGroup1")
                .startNow()//立即生效
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))  //每小时执行
                //.withSchedule(CronScheduleBuilder.cronSchedule("/30 * * * * ?"))  //每5s执行
                .build();

        //4、执行
        scheduler.scheduleJob(jobDetail, cronTrigger);
        System.out.println("--------scheduler start ! ------------");
        scheduler.start();
        System.out.println("--------scheduler shutdown ! ------------");

    }
}