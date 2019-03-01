package com.stylefeng.guns.modular.cesium.controller;

import java.util.ArrayList;
import java.util.List;

import org.postgresql.geometric.PGpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.stylefeng.guns.core.base.controller.BaseController;
import com.stylefeng.guns.modular.cesium.bean.RadarPower;
import com.stylefeng.guns.modular.cesium.service.IRadarInfoService;
import com.stylefeng.guns.modular.cesium.service.IRadarPowerService;
import com.stylefeng.guns.modular.cesium.service.PGService;

@Controller
@RequestMapping("/gisController")
public class ToGisIndexController extends BaseController{
	@Autowired
	private PGService pgService;
	
	@Autowired
	private IRadarPowerService powerService;
	
	@Autowired
	private IRadarInfoService infoService;
	
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
			this.powerService.getPointList(1,121.47861,31.2204949,5.0,1000.0,"shanghai");
			//System.out.println(array);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return "ok！";
    }
	@RequestMapping("/loadRadar")
	@ResponseBody
	public List getRadarInfoList() {
		 List list = this.infoService.getAllList();
		 return list;
	}
	@RequestMapping("/calHeightRWL")
	@ResponseBody
    public List calHeightRadarWLData() {
		List wlist = new ArrayList();
		try {
			//wlist = powerService.getPointList(1,121.47861,31.2204949,5.0,2000.0,"shanghai");
			//JSONArray array= JSONArray.parseArray(JSON.toJSONString(wlist));
		  //  List<String> wlList = new ArrayList<String>();
			//121.47861,31.2204949
			double heightLevel = 70;
			List<List<PGpoint>>  plist = this.powerService.getRadarHeightPower1(1,121.47861,31.2204949,50,2000.0,"shanghai",heightLevel);
			//System.out.println(array)
			
			for(List<PGpoint> list : plist) {
				List tmplist = new ArrayList<>();
				for(PGpoint bean : list) {
					tmplist.add(bean.x);
					tmplist.add(bean.y);
					tmplist.add(heightLevel);
				}
				wlist.add(tmplist);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return wlist;
    }
	@RequestMapping("/wlmerge")
	@ResponseBody
    public List radarPowerMerge() throws Exception {
		List<Integer> radarList = new ArrayList();
		//wlist = powerService.getPointList(1,121.47861,31.2204949,5.0,2000.0,"shanghai");
		//JSONArray array= JSONArray.parseArray(JSON.toJSONString(wlist));
	  //  List<String> wlList = new ArrayList<String>();
		//121.47861,31.2204949 getRadarHeightPower1(1,121.47861,31.2204949,50,2000.0,"shanghai",heightLevel);
		radarList.add(1);
		radarList.add(2);
		double heightlevel = 70;
		//List plist = this.powerService.powerMerge(radarList,heightlevel);
	
		//List plist = this.powerService.powerMergeOf2Radar(1, 2, heightlevel);
		List plist = this.powerService.getFirstRankMergerListOf2Radar(1, 2, heightlevel);
		
	/*	for(List<PGpoint> list : plist) {
			List tmplist = new ArrayList<>();
			for(PGpoint bean : list) {
				tmplist.add(bean.x);
				tmplist.add(bean.y);
				tmplist.add(heightLevel);
			}
			wlist.add(tmplist);
		}
		*/
	
        return plist;
    }
	@RequestMapping("/loadHeightPower")
	@ResponseBody
	public List loadHeightPower(Integer radarid,double heightlevel) {
		List plist = this.powerService.loadHeightPower(radarid,heightlevel);
		return plist;
	}
}
