// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ClosePower.java

package secret.MyJob;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import secret.MQTT.MyMqttClient;
import secret.MyUtil.JDBC_Util;

public class ClosePower
    implements Job
{

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println("Stop Power!!!!!!!!!!!!!!!!!!!!!!");
        MyMqttClient.publishMessage("cmd", "Door_Off", 0);
        JobDataMap dataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        String name = dataMap.getString("name");
        JDBC_Util.del_task(name);
    }
}
