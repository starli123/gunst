package com.stylefeng.guns.modular.cesium.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.Resource;

import org.postgresql.geometric.PGpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.stylefeng.guns.modular.cesium.bean.RadarPower;
import com.stylefeng.guns.modular.cesium.bean.RayBean;
import com.stylefeng.guns.modular.cesium.dao.RadarPowerMapper;
import com.stylefeng.guns.modular.cesium.service.IRadarPowerService;
import com.stylefeng.guns.modular.cesium.util.CoordinateTransferUtil;
import com.stylefeng.guns.modular.cesium.util.PgUtil;
@Service
@Transactional
public class RadarPowerServiceImpl extends ServiceImpl<RadarPowerMapper, RadarPower> implements IRadarPowerService {
	 
	@Resource
	private RadarPowerMapper radarPowerMapper;
	   
	@Override
	public List<RadarPower> selectByRadarId(int radarid) {
		// TODO Auto-generated method stub
		return this.radarPowerMapper.selectByRadarId(radarid);
	}

	@Override
	public void insertRadarPowerBatch(List<RadarPower> radarPowerList) {
		// TODO Auto-generated method stub
		this.radarPowerMapper.insertRadarPowerBatch(radarPowerList);
	}

	@Override
	public void saveRadarPower(RadarPower bean) {
		// TODO Auto-generated method stub
		this.radarPowerMapper.insert(bean);
	}
	@Override
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
			/*
			 for(int key : circleRayMap.keySet()) {
				List list  = circleRayMap.get(key);
			    double[][] realWL = getRealPower(list,radarWL,radarjd,radarwd,radarheight);
			    wlist.add(realWL);
			}
			*/
			List<RadarPower>  entityList = new ArrayList<RadarPower>();
			for(int i = 0;i<radarWL.length;i++) {
				// double[][] realWL = getRealPower(circleRayMap,radarWL[i],radarjd,radarwd,radarheight);
				 List realWL = getRealPowerList(circleRayMap,radarWL[i],radarjd,radarwd,radarheight);
				 String wl = JSON.toJSONString(realWL);
				 RadarPower powerBean = new RadarPower();
				 powerBean.setRadarid(100);
				 powerBean.setYj(radarWL[i][0]);
				 powerBean.setWl(wl);
				 powerBean.setGd(0);
				 entityList.add(powerBean);
			}
			 this.insertBatch(entityList);
			 this.insertPower2GIS(entityList);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return wlist;
	}
	private void insertPower2GIS(List<RadarPower> list) throws SQLException {
		String vals = "";
		for(RadarPower bean : list) {
			String twl = bean.getWl().replaceAll("\\[", "");
			twl = twl.replaceAll("\\]", "");
			String[] strArr = twl.split(",");
			String wlsj="";
			String pointgd ="";
			for(int i=0;i<strArr.length;i++) {
				if((i+1)%3==0) {
					pointgd += strArr[i]+",";
					wlsj = wlsj.substring(0,wlsj.lastIndexOf(" "))+",";
				}else {
					wlsj += strArr[i]+" ";
				}
			}
			//wlsj = wlsj.substring(0,wlsj.lastIndexOf(","));
			pointgd = pointgd.substring(0,pointgd.lastIndexOf(","));
			wlsj ="ST_Polygon(ST_GeomFromEWKT('LINESTRING("+wlsj+strArr[0]+" "+strArr[1]+")'),4326)";
			String tmp = "("+bean.getRadarid()+","+bean.getYj()+","+bean.getGd()+",'"+pointgd+"',"+wlsj+")";
			//String sql = "insert into radarpower(radarid,yj,gd,pointgd,wlgeom) values "+ tmp;
			//System.out.println(sql);
			//PgUtil.executeSql(sql);
			vals += tmp+",";
		}
		
		vals = vals.substring(0,vals.lastIndexOf(","));
		String sql = "insert into radarpower(radarid,yj,gd,pointgd,wlgeom) values "+ vals;
		PgUtil.executeSql(sql);
		PgUtil.destroy();
	}
	private List getRealPowerList(LinkedHashMap<Integer, List> circleRayMap, double[] yjWL, double radarjd,
			double radarwd, double radarheight) {
		// TODO Auto-generated method stub
		CoordinateTransferUtil cTransfer = new CoordinateTransferUtil(radarjd, radarwd, radarheight);
		 double[][] realWL = new double[circleRayMap.keySet().size()][3];
		 List<Double> WLlist = new ArrayList<Double>();
		// circleRayMap.keySet() 从0到359
		 boolean unchanged = true;
		 for(int key : circleRayMap.keySet()) {
			 List list  = circleRayMap.get(key);
			 double[] radarlxWL=cTransfer.polar2WGS84(yjWL[1],key,yjWL[0]);
			 for(Object obj : list) {
				RayBean bean = (RayBean)obj;
				double tmpyj = bean.getYj();
				//理论仰角小于建筑物仰角时，
				if(yjWL[0]<tmpyj ) {
					
					double jl = bean.getSpdis()/Math.cos(Math.PI*yjWL[0]/180);
					if(yjWL[1] > jl) {//且建筑物距离小于威力距离时，经纬度采用建筑物的，高度tan计算。
						WLlist.add(bean.getPoint().x);
						WLlist.add(bean.getPoint().y);
						WLlist.add(bean.getSpdis()*Math.tan(Math.PI*yjWL[0]/180));
					
					}else {
						WLlist.add(radarlxWL[0]);
						WLlist.add(radarlxWL[1]);
						WLlist.add(radarlxWL[2]);
					}
					unchanged = false;
					break;
				}
			 }
			 if(unchanged) {
				 WLlist.add(radarlxWL[0]);
				 WLlist.add(radarlxWL[1]);
				 WLlist.add(radarlxWL[2]);
			 }
		 }
		return WLlist;
	}
	private double[][] getRealPower(LinkedHashMap<Integer, List> circleRayMap, double[] yjWL, double radarjd,
			double radarwd, double radarheight) {
		// TODO Auto-generated method stub
		CoordinateTransferUtil cTransfer = new CoordinateTransferUtil(radarjd, radarwd, radarheight);
		 double[][] realWL = new double[circleRayMap.keySet().size()][3];
		// circleRayMap.keySet() 从0到359
		 boolean unchanged = true;
		 for(int key : circleRayMap.keySet()) {
			 List list  = circleRayMap.get(key);
			 double[] radarlxWL=cTransfer.polar2WGS84(yjWL[1],key,yjWL[0]);
			 for(Object obj : list) {
				RayBean bean = (RayBean)obj;
				double tmpyj = bean.getYj();
				//理论仰角小于建筑物仰角时，
				if(yjWL[0]<tmpyj ) {
					
					double jl = bean.getSpdis()/Math.cos(Math.PI*yjWL[0]/180);
					if(yjWL[1] > jl) {//且建筑物距离小于威力距离时，经纬度采用建筑物的，高度tan计算。
						realWL[key][0]= bean.getPoint().x;
						realWL[key][1]= bean.getPoint().y;
						realWL[key][2]= bean.getSpdis()*Math.tan(Math.PI*yjWL[0]/180);
					}else {
						realWL[key][0]= radarlxWL[0];
						realWL[key][1]= radarlxWL[1];
						realWL[key][2]= radarlxWL[2];
					}
					unchanged = false;
					break;
				}
			 }
			 if(unchanged) {
				realWL[key][0]= radarlxWL[0];
				realWL[key][1]= radarlxWL[1];
				realWL[key][2]= radarlxWL[2];
			 }
		 }
		return realWL;
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
		            if(yj<tmpyj){
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



}
