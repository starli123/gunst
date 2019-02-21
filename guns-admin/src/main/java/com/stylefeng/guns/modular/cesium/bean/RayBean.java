package com.stylefeng.guns.modular.cesium.bean;
import org.postgresql.geometric.PGpoint;
public class RayBean {
	public int id;
	public int rank;
	public double height;
	public double yj;
	public double spdis;
	public PGpoint point;
	
	public RayBean(int id, int rank, double height, double yj, double spdis, PGpoint point) {
		super();
		this.id = id;
		this.rank = rank;
		this.height = height;
		this.yj = yj;
		this.spdis = spdis;
		this.point = point;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getRank() {
		return rank;
	}
	public void setRank(int rank) {
		this.rank = rank;
	}
	public double getHeight() {
		return height;
	}
	public void setHeight(double height) {
		this.height = height;
	}
	public double getYj() {
		return yj;
	}
	public void setYj(double yj) {
		this.yj = yj;
	}
	public double getSpdis() {
		return spdis;
	}
	public void setSpdis(double spdis) {
		this.spdis = spdis;
	}
	public PGpoint getPoint() {
		return point;
	}
	public void setPoint(PGpoint point) {
		this.point = point;
	}
	
}
