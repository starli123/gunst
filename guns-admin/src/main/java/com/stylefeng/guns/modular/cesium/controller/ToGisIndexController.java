package com.stylefeng.guns.modular.cesium.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.stylefeng.guns.core.base.controller.BaseController;
import com.stylefeng.guns.modular.cesium.bean.RadarPower;
import com.stylefeng.guns.modular.cesium.service.IRadarPowerService;
import com.stylefeng.guns.modular.cesium.service.PGService;

@Controller
@RequestMapping("/gisController")
public class ToGisIndexController extends BaseController{
	@Autowired
	private PGService pgService;
	
	@Autowired
	private IRadarPowerService powerService;
	
	private String PREFIX = "/cesium/";
	@RequestMapping("/index")
    public String deptAdd() {
        return PREFIX + "index10.html";
    }
	@RequestMapping("/test")
    public String ddtest() {
        return PREFIX + "index5.html";
    }
	@RequestMapping("/getRWL")
	@ResponseBody
    public List getRadarWLData() {
		List wlist = new ArrayList();
		try {
			//wlist = powerService.getPointList(1,121.47861,31.2204949,5.0,2000.0,"shanghai");
			//JSONArray array= JSONArray.parseArray(JSON.toJSONString(wlist));
		  //  List<String> wlList = new ArrayList<String>();
			List<RadarPower> list= this.powerService.selectByRadarId(100);
			if(list == null || list.size() == 0)
				return null;
		    for(RadarPower bean : list) {
		    	wlist.add(bean.getWl());
		    }
			//System.out.println(array);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return wlist;
    }
	@RequestMapping("/calRWL")
	@ResponseBody
    public String calRadarWLData() {
		List wlist = new ArrayList();
		try {
			//wlist = powerService.getPointList(1,121.47861,31.2204949,5.0,2000.0,"shanghai");
			//JSONArray array= JSONArray.parseArray(JSON.toJSONString(wlist));
		  //  List<String> wlList = new ArrayList<String>();
			this.powerService.getPointList(1,121.47861,31.2204949,5.0,2000.0,"shanghai");
			//System.out.println(array);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return "ok！";
    }

}
