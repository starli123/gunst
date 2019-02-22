package com.stylefeng.guns.modular.cesium.service;

import java.util.List;

import com.baomidou.mybatisplus.service.IService;
import com.stylefeng.guns.modular.cesium.bean.RadarInfo;


public interface IRadarInfoService extends IService<RadarInfo>{
	public List<RadarInfo> getAllList();
}
