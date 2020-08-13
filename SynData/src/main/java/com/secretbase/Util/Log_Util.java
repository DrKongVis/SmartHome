package com.secretbase.Util;

public class Log_Util {
    private static boolean isDebug = true;

    public static void info(String info){
        if(isDebug){
            System.out.println("info:"+info);
        }
    }
    public static void error(String info){
        if(isDebug){
            System.out.println("error:"+info);
        }
    }
    public static void warning(String info){
        if(isDebug){
            System.out.println("warning:"+info);
        }
    }
}
