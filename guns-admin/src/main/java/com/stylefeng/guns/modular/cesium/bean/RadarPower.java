package com.stylefeng.guns.modular.cesium.bean;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;

@TableName("dktc_radarpower")
public class RadarPower {
	 private static final long serialVersionUID = 1L;
	 
	@TableId(value="id", type= IdType.AUTO)
	public int id;
	public int radarid;
	public double yj;
	public double gd;
	public String wl;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getRadarid() {
		return radarid;
	}
	public void setRadarid(int radarid) {
		this.radarid = radarid;
	}
	public double getYj() {
		return yj;
	}
	public void setYj(double yj) {
		this.yj = yj;
	}
	public double getGd() {
		return gd;
	}
	public void setGd(double gd) {
		this.gd = gd;
	}
	public String getWl() {
		return wl;
	}
	public void setWl(String wl) {
		this.wl = wl;
	}
	
	
	

}
