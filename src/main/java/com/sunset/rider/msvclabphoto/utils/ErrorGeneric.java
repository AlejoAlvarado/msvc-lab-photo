package com.sunset.rider.msvclabphoto.utils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public  class ErrorGeneric {

    public static Map<String,String> error(String message){
        Map<String,String> error = new HashMap<>();
        error.put("error",message);
        error.put("timestamp", LocalDateTime.now().toString());
        return  error;
    }
}
