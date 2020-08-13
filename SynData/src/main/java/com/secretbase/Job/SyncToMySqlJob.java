package com.secretbase.Job;

import com.secretbase.Util.JDBC_Util;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务：将温湿度的值同步到Mysql数据库
 */
public class SyncToMySqlJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        System.out.println("Job:同步温湿度");
        JDBC_Util.sync();
        System.out.println("\n*****  同步完成  *****");
    }


}
