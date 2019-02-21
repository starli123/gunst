package com.stylefeng.guns.modular.cesium.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.stylefeng.guns.modular.cesium.bean.RadarInfo;
import com.stylefeng.guns.modular.cesium.bean.RadarPower;

public interface RadarPowerMapper extends BaseMapper<RadarPower> {
	 List<RadarPower> selectByRadarId(@Param("radarid") int radarid);
	 int insertRadarPowerBatch(List<RadarPower > radarPowerList);
	 int saveRadarPower(RadarPower bean);
}
