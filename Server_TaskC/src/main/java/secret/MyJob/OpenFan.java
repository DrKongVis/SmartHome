// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   OpenFan.java

package secret.MyJob;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import secret.MQTT.MyMqttClient;

public class OpenFan implements Job {


    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println("Start Fan!!!!!!!!!!!");
        MyMqttClient.publishMessage("cmd", "Fan_On", 0);
    }
}
