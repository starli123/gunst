package com.stylefeng.guns.modular.cesium.service;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;

import org.junit.Before;
import org.junit.Test;
import org.postgresql.geometric.PGcircle;
import org.postgresql.geometric.PGpolygon;
import org.postgresql.geometric.PGpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stylefeng.guns.modular.cesium.bean.RadarPower;
import com.stylefeng.guns.modular.cesium.bean.RayBean;
import com.stylefeng.guns.modular.cesium.dao.RadarPowerMapper;
import com.stylefeng.guns.modular.cesium.util.PgUtil;
@Service
@Transactional
public class PGService extends ServiceImpl<RadarPowerMapper, RadarPower> {
	
	public void generateRay(double radarid,double jd,double wd,double maxdis) {
		String sql ="insert into radarray"
				+ "SELECT\r\n" + 
				"		t.n as id,\r\n" + 
				 		radarid +",\r\n"+
				"		ST_SetSRID(\r\n" + 
				"			ST_MakeLine(\r\n" + 
				"				ST_SetSRID(ST_MakePoint("+jd+" ,"+wd+"),4326),\r\n" + 
				"				ST_Project(\r\n" + 
				"				   ST_SetSRID(ST_MakePoint("+jd+" ,"+wd+"),4326),\r\n" + 
				"				   "+maxdis+" + 1,\r\n" + 
				"				   radians(0 +  n::numeric *1)\r\n" + 
				"				)::geometry\r\n" + 
				"			),\r\n" + 
				"		 4326) AS geom\r\n" + 
				"		FROM generate_series(0, 359) as t(n)";
		Statement stmt;
		try {
			stmt = PgUtil.getConn().createStatement();
			stmt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	//sql 优化 筛选出 比第一个大的，比前面一个大的。用窗口函数
	public List getPointList(int radarid,double radarjd,double radarwd,double radarheight,double maxdis,String tabname) throws Exception {
	    LinkedHashMap<Integer,List> circleRayMap = new LinkedHashMap<Integer,List>();
	    List wlist = new ArrayList();
		double[][] radarWL = {
	            {0, 0},
	            {5, 100},
	            {7, 150},
	            {10,200},
	            {12.5, 280},
	            {15,350},
	            {17.5, 430},
	            {20,500},
	            {22.5, 580},
	            {25,650},
	            {27.5, 710},
	            {30,780},
	            {32.5, 800},
	            {35,820},
	            {37.5, 850},
	            {40,870},
	            {42.5, 890},
	            {45,900},
	            {47.5, 880},
	            {50,820},
	            {52.5, 760},
	            {55,700},
	            {57.5, 650},
	            {60,600},
	            {62.5, 550},
	            {65,500},
	            {67.5, 450},
	            {70,400},
	            {72.5, 300},
	            {75,260},
	            {77.5, 200},
	            {80,150},
	            {82.5, 120},
	            {85,100},
	            {87.5, 50},
	            {90,0}
	            };
		String center = "ST_SetSRID(ST_MakePoint("+radarjd+" ,"+radarwd+"),4326)";
		String sql = " SELECT id,height,"+center+"::geography <-> ST_Transform(point,4326) as dis , ST_AsText(point) as geom," + 
					"atand(height/("+center+"::geography <-> st_transform(point,4326)::geography)) as yj ,"+
				    " row_number() over(partition by id order by "+center+"::geography <-> point::geography) as ranking" + 
					" FROM " + 
					" (SELECT  r.id,  b.height, (ST_Dump(ST_Intersection(ST_Boundary(b.geom),r.geom))).geom AS point FROM " + 
					"  radarray r LEFT JOIN (select height,st_transform(geom,4326) as geom  from "+tabname+" where height>="+radarheight+" and ST_DWithin(st_transform("+center+",32651),geom, "+maxdis+")) b on ST_Intersects(b.geom,r.geom)" + 
					" )g";
		Statement stmt;
		try {
			stmt = PgUtil.getConn().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()) {
				int id = rs.getInt(1);
				double height = rs.getDouble(2);
				double dis = rs.getDouble(3);
				String pointstr = rs.getString(4);
				String[] strArr1 = pointstr.split("\\(");
				String[] strArr2 = strArr1[1].split("\\)");
				String[] strArr3 = strArr2[0].split(" ");
				PGpoint point = new PGpoint(Double.valueOf(strArr3[0]), Double.valueOf(strArr3[1]));
				double yj = rs.getDouble(5);
				int rank = rs.getInt(6);
				RayBean bean  = new RayBean(id,rank,height,yj,dis,point);
				if(!circleRayMap.containsKey(id)) {
					List list =  new ArrayList();
					list.add(bean);
					circleRayMap.put(id, list);
				}else {
					List list = circleRayMap.get(id);
					if(list == null) {
						list = new ArrayList();
						list.add(bean);
						circleRayMap.put(id, list);
					}else if(list.size() ==0) {
						list.add(bean);
						circleRayMap.put(id, list);
					}else {
						RayBean bb = (RayBean) list.get(list.size()-1);
						if(bb.getYj() < bean.getYj()) {
							list.add(bean);
							circleRayMap.put(id, list);
						}
					}
				
				}
			}
			
			//比较生成list；circleRayMap.keySet().size() == 360
			for(int key : circleRayMap.keySet()) {
				List list  = circleRayMap.get(key);
			    double[][] realWL = getRealPower(list,radarWL);
			    wlist.add(realWL);
			}
			

			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return wlist;
	}
	public  Object byte2obj(byte[] bytes) throws Exception {
	    Object ret = null;
	    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
	    ObjectInputStream in = new ObjectInputStream(bais);
	    ret = in.readObject();
	    in.close();
	    return ret;
	}
	public void getData() throws SQLException {
		Statement stmt = PgUtil.getConn().createStatement();
		ResultSet rs = stmt.executeQuery("select * from shanghai");
		while(rs.next()) {
			String wkt = rs.getString(3);
			System.out.println("wkt = " + wkt);
		}
		
		
	}
	private double[][] getRealPower(List list,double[][] radarWL){
        double[][] realWL = new	double[radarWL.length][2];	
        for(int i = 0;i < radarWL.length;i++){
            for(int j = 0;j < radarWL[i].length;j++){
            	realWL[i][j] = radarWL[i][j];
            }
        }

		for(Object obj : list) {
			RayBean bean = (RayBean)obj;
			double tmpyj = bean.getYj();
	        int k = 0;
			for(int j = k;j< realWL.length;j++){
				  double yj = realWL[j][0];
		            if(yj>tmpyj){
		                //前面的都要修改
		               for(int m =k; m < j; m++){
		                double dqyj = realWL[m][0];
		                realWL[m][1] = Math.min(realWL[m][1],bean.getSpdis()/Math.cos(Math.PI*dqyj/180));
		               }
		               k=j;
		               break;
		            }
			}
			if(k+1>=realWL.length){
		           break;
		     }
		}
		return realWL;
	}
	public void insertCircle() throws SQLException {
 
		PGpoint center = new PGpoint(1, 2.5);
		// PGpolygon polygon = new PGpolygon(points);
		double radius = 4;
		PGcircle circle = new PGcircle(center, radius);
 
		PreparedStatement ps = PgUtil.getConn().prepareStatement("INSERT INTO geomtest(mycirc) VALUES (?)");
		ps.setObject(1, circle);
		ps.executeUpdate();
		ps.close();
	}
 
	
	public void retrieveCircle() throws SQLException {
		Statement stmt = PgUtil.getConn().createStatement();
		ResultSet rs = stmt.executeQuery("SELECT mycirc, area(mycirc) FROM geomtest");
		rs.next();
		PGcircle circle = (PGcircle) rs.getObject(1);
		double area = rs.getDouble(2);
		// PG
 
		PGpoint center = circle.center;
		double radius = circle.radius;
 
		System.out.println("Center (X, Y) = (" + center.x + ", " + center.y + ")");
		System.out.println("Radius = " + radius);
		System.out.println("Area = " + area);
	}
 
	
	public void getWKTFromGEOM() throws SQLException {
		Statement stmt = PgUtil.getConn().createStatement();
		ResultSet rs = stmt.executeQuery("SELECT ST_AsText(geom) FROM sheng where name = '新疆维吾尔自治区'");
		rs.next();
		String wkt = rs.getString(1);
		System.out.println(wkt);
	}
 
	
	public void getWKBFromGEOM() throws SQLException {
		Statement stmt = PgUtil.getConn().createStatement();
		ResultSet rs = stmt.executeQuery("SELECT ST_AsBinary(geom) FROM sheng");
		rs.next();
		String wkt = rs.getString(1);
		System.out.println("wkt = " + wkt);
	}
 
	
	public void getEWKTFromGEOM() throws SQLException {
		Statement stmt = PgUtil.getConn().createStatement();
		ResultSet rs = stmt.executeQuery("SELECT ST_AsEWKT(geom) FROM sheng");
		rs.next();
		String wkt = rs.getString(1);
		System.out.println("wkt = " + wkt);
	}
	
	
	public void getBufferFromGEOM() throws SQLException {
		Statement stmt = PgUtil.getConn().createStatement();
		ResultSet rs = stmt.executeQuery("SELECT ST_AsEWKT(ST_Buffer(geom, 0.2)), ST_AsEWKT(geom) FROM sheng where id = 1");
		rs.next();
		String wkt = rs.getString(1);
		String wkt2 = rs.getString(2);
		//System.out.println("tt " + wkt.substring(0, 100));
		//System.out.println("tt " + wkt2.substring(0, 100));
	}
	
	/**
	 * 需要在postGis中模拟一个dual表（注意指明空间字段）
	 */

	public void getBufferFromWKT() throws SQLException {
		// 新疆维吾尔自治区
		String wkt = "";
				//PropertiesUtility.getInstance().findFileValue("system.properties", "wkt");
		long startTime = new java.util.Date().getTime();
		System.out.println(startTime);
		String sql = "SELECT ST_AsText(ST_Buffer(st_geomfromtext('" + wkt + "'), 0.2)) FROM dual";
		Statement stmt = PgUtil.getConn().createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		rs.next();
 
		long endTime = new java.util.Date().getTime();
		System.out.println(endTime);
		System.out.println("缓冲时间：" + (endTime - startTime));
		System.out.println("坐标点数：" + wkt.split(",").length);
	}
 
	/**
	 * 根据指定的缓冲距离进行缓冲，以米为单位
	 */

	public void getBufferFromWKT2() throws SQLException {
		// 新疆维吾尔自治区  wkt=MULTIPOLYGON(((79.036744 34.3364.............
		String wkt = "";
			//PropertiesUtility.getInstance().findFileValue("system.properties", "wkt");
		long startTime = new java.util.Date().getTime();
		System.out.println(startTime);
		// 缓冲距离为10.8KM
		String sql = "SELECT ST_AsText(st_transform(st_setsrid(ST_Buffer(st_transform(st_setsrid(st_geomfromtext('" + wkt
				+ "'), 4326), 2333), 10800), 2333), 4326)) FROM dual";
		Statement stmt = PgUtil.getConn().createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		rs.next();
		String resultWKT = rs.getString(1);
 
		System.out.println(resultWKT);
 
		long endTime = new java.util.Date().getTime();
		System.out.println(endTime);
		System.out.println("缓冲时间：" + (endTime - startTime));
		System.out.println("坐标点数：" + wkt.split(",").length);
	}
 
	// 判断点是否在多边形内

	public void getWithin() throws SQLException {
		Statement stmt = PgUtil.getConn().createStatement();
		String sql = "SELECT name FROM sheng where ST_Within(ST_MakePoint(116.561, 40.276), geom)";
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			String wkt = rs.getString(1);
			System.out.println(wkt);
		}
	}
}
