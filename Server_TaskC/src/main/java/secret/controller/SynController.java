package secret.controller;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import secret.MQTT.MyMqttClient;
import secret.MyJob.*;
import secret.MyUtil.JDBC_Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@Controller
public class SynController {

    private Scheduler scheduler;

    @ResponseBody
    @RequestMapping("/main")
    public String main() throws SchedulerException {
        //初始化数据库连接
        JDBC_Util.Init_Jdbc();
        MyMqttClient.start("SmartHomeServer_Task:"+ new Random().nextInt());
        //JobScheduler.start();
        System.out.println("正在等待接收");
        scheduler = new StdSchedulerFactory().getScheduler();
        ReceiveCMD_TCP();
        return "Start Success";
    }

    public void ReceiveCMD_TCP() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    ServerSocket serverSocket = new ServerSocket(1234);
                    do
                    {
                        System.out.println("等待设备接入端口1234");
                        Socket server = serverSocket.accept();
                        DataInputStream in = new DataInputStream(server.getInputStream());
                        String cmd = in.readUTF();
                        DataOutputStream out = new DataOutputStream(server.getOutputStream());
                        if(cmd.equals("StartTask"))
                        {
                            out.writeUTF("StartTask!!!!!!!!!!!!!!!");
                            startTask_Job();
                            //return;
                        }
                        System.out.println(cmd);
                        server.close();
                    } while(true);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public String startTask_Job() throws Exception {
        Statement stmt = JDBC_Util.conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT  Taskname,Tasktype,startTime,endTime FROM Task");

        while(rs.next()){
            String startTime = rs.getString("startTime");
            String endTime = rs.getString("endTime");
            int flag = rs.getInt("Tasktype");
            String name = rs.getString("Taskname");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    StartTask(startTime, flag);
                    StopTask(endTime, flag, name);
                }
            }).start();
        }
        return (new StringBuilder()).append("startTask success").toString();
    }

    public void StartTask(String startTime, int flag)
    {
        try {
            Date startDate;
            JobDetail jobDetail;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                startDate = format.parse(startTime);
            System.out.println("开始："+startDate.toString());
            if(flag == 0)
                jobDetail = JobBuilder.newJob(OpenFan.class).withIdentity((new StringBuilder()).append("job1").append((new Date()).getTime()).toString(), "group1").build();
            else
            if(flag == 1)
                jobDetail = JobBuilder.newJob(OpenLight.class).withIdentity((new StringBuilder()).append("job2").append((new Date()).getTime()).toString(), "group1").build();
            else
            if(flag == 2)
                jobDetail = JobBuilder.newJob(OpenPower.class).withIdentity((new StringBuilder()).append("job3").append((new Date()).getTime()).toString(), "group1").build();
            else
                return;
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity((new StringBuilder()).append("trigger1").append((new Date()).getTime()).toString(), "triggerGroup1").startAt(startDate).build();
            scheduler.scheduleJob(jobDetail, trigger);
            System.out.println("--------scheduler start ! ------------");
            scheduler.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    public void StopTask(String endTime, int flag, String name) {
        try {
            Date endDate;
            JobDetail jobDetail;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            endDate = format.parse(endTime);
            System.out.println("结束："+endDate.toString());
            if(flag == 0)
                jobDetail = JobBuilder.newJob(CloseFan.class).withIdentity((new StringBuilder()).append("job1").append((new Date()).getTime()).toString(), "group2").usingJobData("name", name).build();
            else
            if(flag == 1)
                jobDetail = JobBuilder.newJob(CloseLight.class).withIdentity((new StringBuilder()).append("job2").append((new Date()).getTime()).toString(), "group2").usingJobData("name", name).build();
            else
            if(flag == 2)
                jobDetail = JobBuilder.newJob(ClosePower.class).withIdentity((new StringBuilder()).append("job3").append((new Date()).getTime()).toString(), "group2").usingJobData("name", name).build();
            else
                return;
           Trigger trigger = TriggerBuilder.newTrigger().withIdentity((new StringBuilder()).append("trigger1").append((new Date()).getTime()).toString(), "triggerGroup2").startAt(endDate).build();
                scheduler.scheduleJob(jobDetail, trigger);
                System.out.println("--------scheduler start ! ------------");
                scheduler.start();
        }catch (Exception e){
            e.printStackTrace();
        }
       return;
    }

}
