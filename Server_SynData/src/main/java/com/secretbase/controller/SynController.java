package com.secretbase.controller;

import com.secretbase.Job.JobScheduler;
import com.secretbase.MQTT.MyMqttClient;
import com.secretbase.Util.JDBC_Util;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Random;

@Controller
public class SynController {

    @ResponseBody
    @RequestMapping("/main")
    public String main() throws SchedulerException {
        //初始化数据库连接
        JDBC_Util.Init_Jdbc();
        MyMqttClient.start("SmartHomeServer:"+ new Random().nextInt());
        JobScheduler.start();
        return "Start Success";
    }
}
