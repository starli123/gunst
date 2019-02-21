package com.stylefeng.guns.modular.cesium.service;

import java.util.List;
import com.baomidou.mybatisplus.service.IService;
import com.stylefeng.guns.modular.cesium.bean.RadarPower;

public interface IRadarPowerService extends IService<RadarPower> {
	public List<RadarPower> selectByRadarId(int radarid);
	public void insertRadarPowerBatch(List<RadarPower > radarPowerList);
	public void saveRadarPower(RadarPower bean);
	public List getPointList(int radarid,double radarjd,double radarwd,double radarheight,double maxdis,String tabname) throws Exception;
	
}
