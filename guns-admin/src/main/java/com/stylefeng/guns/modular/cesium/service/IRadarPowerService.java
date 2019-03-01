package com.stylefeng.guns.modular.cesium.service;

import java.util.List;
import com.baomidou.mybatisplus.service.IService;
import com.stylefeng.guns.modular.cesium.bean.RadarPower;

public interface IRadarPowerService extends IService<RadarPower> {
	public List<RadarPower> selectByRadarId(int radarid);
	public void insertRadarPowerBatch(List<RadarPower > radarPowerList);
	public void saveRadarPower(RadarPower bean);
	public List getPointList(int radarid,double radarjd,double radarwd,double radarheight,double maxdis,String tabname) throws Exception;
	public List getRadarHeightPower(int radarid,double radarjd,double radarwd,double radarheight,double maxdis,String tabname,double wlheight) throws Exception;
	public List getRadarHeightPower1(int radarid,double radarjd,double radarwd,double radarheight,double maxdis,String tabname,double wlheight) throws Exception;
	public List getRadarHeightPower2(int radarid,double radarjd,double radarwd,double radarheight,double maxdis
				,String tabname,double wlheight) throws Exception;
	public List powerMerge(List<Integer> radarList,double heightlevel) throws Exception;
	public List powerMergeOf2Radar(Integer radarid1, Integer radarid2, double heightlevel);
	List getFirstRankMergerListOf2Radar(Integer radarid1, Integer radarid2, double heightlevel);
	public List loadHeightPower(Integer radarid, double heightlevel);
}
