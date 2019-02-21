package com.stylefeng.guns.modular.cesium.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.stylefeng.guns.modular.cesium.bean.RadarInfo;


public interface RadarInfoMapper extends BaseMapper<RadarInfo> {
    List<RadarInfo> selectById(@Param("id") String id);
}
