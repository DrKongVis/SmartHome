// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   OpenLight.java

package secret.MyJob;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import secret.MQTT.MyMqttClient;

public class OpenLight implements Job {


    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println("Start Light!!!!!!!!!!!");
        MyMqttClient.publishMessage("cmd", "Light_On", 0);
    }
}
