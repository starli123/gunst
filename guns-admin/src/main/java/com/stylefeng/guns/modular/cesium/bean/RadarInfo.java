package com.stylefeng.guns.modular.cesium.bean;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
@TableName("dktc_radarinfo")
public class RadarInfo {
	 private static final long serialVersionUID = 1L;
    @TableId(value="id", type= IdType.AUTO)
    public int id;
    public String name;
    public String xh;
    public double jd;
    public double wd;
    public double gd;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getXh() {
		return xh;
	}
	public void setXh(String xh) {
		this.xh = xh;
	}
	public double getJd() {
		return jd;
	}
	public void setJd(double jd) {
		this.jd = jd;
	}
	public double getWd() {
		return wd;
	}
	public void setWd(double wd) {
		this.wd = wd;
	}
	public double getGd() {
		return gd;
	}
	public void setGd(double gd) {
		this.gd = gd;
	}
	  
}
