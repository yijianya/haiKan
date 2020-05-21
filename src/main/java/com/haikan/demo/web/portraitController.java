package com.haikan.demo.web;

import com.alibaba.fastjson.JSONObject;
import com.haikan.demo.util.haiKanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: jianhua-hu
 * @Date: 2019/11/25 11:28
 * @Version 1.0
 */
@RestController
public class portraitController {

    @RequestMapping("/getList")
    public JSONObject getList() {
        System.out.println("contoller");
        haiKanUtil test = new haiKanUtil();
        return test.getfacePicURL();
    }

    @RequestMapping("/getTest")
    public  JSONObject getTest(){

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sccess",false);

        jsonObject.put("data",new String[10]);
        jsonObject.put("msg","错误");
        return jsonObject;
    }

}
