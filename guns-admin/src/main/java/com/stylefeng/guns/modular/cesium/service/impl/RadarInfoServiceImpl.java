package com.stylefeng.guns.modular.cesium.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.stylefeng.guns.modular.cesium.bean.RadarInfo;
import com.stylefeng.guns.modular.cesium.dao.RadarInfoMapper;
import com.stylefeng.guns.modular.cesium.service.IRadarInfoService;
@Service
@Transactional
public class RadarInfoServiceImpl extends ServiceImpl<RadarInfoMapper, RadarInfo> implements IRadarInfoService{
	@Resource
	private RadarInfoMapper radarInfoMapper;
	
	public List<RadarInfo> getAllList(){
		List<RadarInfo> list = this.radarInfoMapper.getAllRadar();
		return list;
	}
}
