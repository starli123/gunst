package com.stylefeng.guns.modular.cesium.service.impl;

import static org.mockito.Matchers.doubleThat;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.postgresql.geometric.PGpoint;
import org.postgresql.geometric.PGpolygon;
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
/*
 * 高度层WL计算
 * @see com.stylefeng.guns.modular.cesium.service.IRadarPowerService#getRadarHeightPower(int, double, double, double, double, java.lang.String, double)
 */
	@Override
	public List getRadarHeightPower(int radarid,double radarjd,double radarwd,double radarheight,double maxdis
			,String tabname,double wlheight) throws Exception{
		// TODO Auto-generated method stub
		List<List<PGpoint>>  plist = new  ArrayList<List<PGpoint>>();
		CoordinateTransferUtil cTransfer = new CoordinateTransferUtil(radarjd, radarwd, radarheight);
		  LinkedHashMap<Integer,RayBean> circleRayMap = new LinkedHashMap<Integer,RayBean>();
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
			
			/*String center = "ST_SetSRID(ST_MakePoint("+radarjd+" ,"+radarwd+"),4326)";
			String sql = " SELECT id,height,ST_distance("+center+"::geography,ST_Transform(point,4326)::geography) as dis , ST_AsText(point) as geom," + 
						"atand(height/ST_distance("+center+"::geography,st_transform(point,4326)::geography)) as yj ,"+
					    " row_number() over(partition by id order by atand(height/("+center+"::geography <-> point::geography)) desc) as ranking" + 
						" FROM " + 
						" (SELECT  r.id,  b.height, (ST_Dump(ST_Intersection(ST_Boundary(b.geom),r.geom))).geom AS point FROM " + 
						"  radarray r LEFT JOIN (select height,st_transform(geom,4326) as geom  from "+tabname+" where height >= "+radarheight+" and ST_DWithin(st_transform("+center+",32651),geom, "+maxdis+")) b on ST_Intersects(b.geom,r.geom)" + 
						" )g";
			*/
			String center = "ST_SetSRID(ST_MakePoint("+radarjd+" ,"+radarwd+"),4326)";
			String sql = " SELECT id,height,"+center+"::geography <-> ST_Transform(point,4326) as dis , ST_AsText(point) as geom," + 
						"atand(height/("+center+"::geography <-> st_transform(point,4326)::geography)) as yj ,"+
					    " row_number() over(partition by id order by "+center+"::geography <-> point::geography) as ranking" + 
						" FROM " + 
						" (SELECT  r.id,  b.height, (ST_Dump(ST_Intersection(ST_Boundary(b.geom),r.geom))).geom AS point FROM " + 
						"  radarray r LEFT JOIN (select height,st_transform(geom,4326) as geom  from "+tabname+" where height>="+wlheight+" and ST_DWithin(st_transform("+center+",32651),geom, "+maxdis+")) b on ST_Intersects(b.geom,r.geom)" + 
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
					if(bean.getRank() == 1) {
						circleRayMap.put(id, bean);
			
					}
			
				}
				//plist.add(testlist);
				
				
				//比较生成list；circleRayMap.keySet().size() == 360
				/*
				 for(int key : circleRayMap.keySet()) {
					List list  = circleRayMap.get(key);
				    double[][] realWL = getRealPower(list,radarWL,radarjd,radarwd,radarheight);
				    wlist.add(realWL);
				}
				*/
				plist = getHeightLLPoint(radarjd, radarwd, radarheight, radarWL, wlheight);
				PGpoint centerPoint = new PGpoint(radarjd, radarwd);
				 for(int key : circleRayMap.keySet()) {
					 RayBean bean  = circleRayMap.get(key);
					 double dis = getDistanceByGpoint(bean.getPoint(),centerPoint)*1000;
					 for(List<PGpoint> blist : plist) {
						double tmpdis = getDistanceByGpoint(blist.get(key),centerPoint)*1000;
						if(tmpdis <= dis) {
							break;
						}else {
							
							blist.get(key).x = bean.getPoint().x;
							blist.get(key).y = bean.getPoint().y;
						}
					 }
				     
				 }
				
			/*	PGpoint centerPoint = new PGpoint(radarjd, radarwd);
				 for(int key : circleRayMap.keySet()) {
					 RayBean bean  = circleRayMap.get(key);
					 double r = wlheight / Math.sin(Math.PI*bean.getYj()/180);
					 double dis = wlheight / Math.tan(Math.PI*bean.getYj()/180);
					 double [] dd = cTransfer.radar2Wgs84(r, key, wlheight);
					 for(List<PGpoint> blist : plist) {
						double tmpdis = getDistanceByGpoint(blist.get(key),centerPoint)*1000;
						if(tmpdis <= dis) {
							continue;
						}else {
							
							blist.get(key).x = dd[0];
							blist.get(key).y = dd[1];
						}
					 }
				     
				 }
				 */
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return plist;
	}
	
	private double getDistanceByGpoint(PGpoint p1,PGpoint p2) {
		return Math.abs(CoordinateTransferUtil.distance(p1.x, p1.y, p2.x, p2.y));
	}
	private LinkedHashMap<Integer,List<RayBean>> getCircleMap(int radarid,double radarjd,double radarwd,double radarheight,double maxdis
			,String tabname,double wlheight){
		
		
		CoordinateTransferUtil cTransfer = new CoordinateTransferUtil(radarjd, radarwd, radarheight);
		  LinkedHashMap<Integer,List<RayBean>> circleRayMap = new LinkedHashMap<Integer,List<RayBean>>();
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
			
			/*String center = "ST_SetSRID(ST_MakePoint("+radarjd+" ,"+radarwd+"),4326)";
			String sql = " SELECT id,height,ST_distance("+center+"::geography,ST_Transform(point,4326)::geography) as dis , ST_AsText(point) as geom," + 
						"atand(height/ST_distance("+center+"::geography,st_transform(point,4326)::geography)) as yj ,"+
					    " row_number() over(partition by id order by atand(height/("+center+"::geography <-> point::geography)) desc) as ranking" + 
						" FROM " + 
						" (SELECT  r.id,  b.height, (ST_Dump(ST_Intersection(ST_Boundary(b.geom),r.geom))).geom AS point FROM " + 
						"  radarray r LEFT JOIN (select height,st_transform(geom,4326) as geom  from "+tabname+" where height >= "+radarheight+" and ST_DWithin(st_transform("+center+",32651),geom, "+maxdis+")) b on ST_Intersects(b.geom,r.geom)" + 
						" )g";
			*/
			String center = "ST_SetSRID(ST_MakePoint("+radarjd+" ,"+radarwd+"),4326)";
			String sql = " SELECT id,height,"+center+"::geography <-> ST_Transform(point,4326) as dis , ST_AsText(point) as geom," + 
						"atand(height/("+center+"::geography <-> st_transform(point,4326)::geography)) as yj ,"+
					    " row_number() over(partition by id order by "+center+"::geography <-> point::geography) as ranking" + 
						" FROM " + 
						" (SELECT  r.id,  b.height, (ST_Dump(ST_Intersection(ST_Boundary(b.geom),r.geom))).geom AS point FROM " + 
						"  (select * from radarray where radarid="+radarid+") r LEFT JOIN (select height,st_transform(geom,4326) as geom  from "+tabname+" where height>="+radarheight+" and ST_DWithin(st_transform("+center+",32651),geom, "+maxdis+")) b on ST_Intersects(b.geom,r.geom)" + 
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
			}catch(Exception e) {
				
			}
			return circleRayMap;
	}
	/*
	 * 返回每一圈的点的列表
	 * 存储变化的相邻两个点，插值计算出当前高度层的数据
	 */
	private List<List<PGpoint>> getHeightLLPoint(double radarjd,double radarwd,double radarheight,double[][] radarWL,double heightLevel){
		
		List<List<PGpoint>> list = new ArrayList<List<PGpoint>>();
	   
		double[] lastWL = new double[2];
		lastWL = radarWL[0];
		boolean isLess = true;
		for(int i = 1 ; i < radarWL.length ;i++) {
			double tmpGd = Math.sin((Math.PI*radarWL[i][0]) /180) * radarWL[i][1];
			if(tmpGd < heightLevel &&  !isLess) {//穿过一次
				double jl = getJLByCZ(lastWL, radarWL[i], heightLevel);
				List<PGpoint> tlist = getListByJLGD(radarjd, radarwd, radarheight, jl, heightLevel);
				list.add(tlist);
				isLess= true;
			}else if(tmpGd > heightLevel && isLess) {//穿过一次
				double jl = getJLByCZ(lastWL, radarWL[i], heightLevel);
				List<PGpoint> tlist = getListByJLGD(radarjd, radarwd, radarheight, jl, heightLevel);
				list.add(tlist);
				isLess = false;
			}if(tmpGd == heightLevel) {
				if(i+1<radarWL.length) {
					double tmpGd2 = Math.sin((Math.PI*radarWL[i+1][0]) /180) * radarWL[i+1][1];
					if(tmpGd2 < heightLevel && !isLess) { //穿过一次
						List<PGpoint> tlist = getListByFWJL(radarjd, radarwd, radarheight, radarWL[i][0], radarWL[i][1]);
						list.add(tlist);
						isLess = true;
					}else if(tmpGd2 > heightLevel && isLess) {//穿过一次
						List<PGpoint> tlist = getListByFWJL(radarjd, radarwd, radarheight, radarWL[i][0], radarWL[i][1]);
						list.add(tlist);
						isLess = false;
					}
				}

			}
			lastWL = radarWL[i];
		}
		return list;
	}
	private double getJLByCZ(double[] p1,double[] p2,double height) {
		double jl =0;
		double tmpGd1 = Math.sin((Math.PI*p1[0]) /180) * p1[1];
		double tmpGd2 = Math.sin((Math.PI*p2[0]) /180) * p2[1];
		jl =((p1[1]-p2[1])/(tmpGd1-tmpGd2))*(height-tmpGd1) + p1[1];
		return  jl;
	}
    private List<PGpoint> getListByFWJL(double radarjd,double radarwd,double radarheight,double yj,double jl){
    	List<PGpoint> list = new ArrayList<PGpoint>();
    	CoordinateTransferUtil cTransfer = new CoordinateTransferUtil(radarjd, radarwd, radarheight);
    	for(int a = 0 ;a < 360 ; a++) {
    		double[] jwg = cTransfer.polar2WGS84(jl, a, yj);
    		PGpoint point = new PGpoint(jwg[0], jwg[1]);
    		list.add(point);
    	}
    	
    	return list;
    
    }
    
    private List<PGpoint> getListByJLGD(double radarjd,double radarwd,double radarheight,double jl,double gd){
    	List<PGpoint> list = new ArrayList<PGpoint>();
    	CoordinateTransferUtil cTransfer = new CoordinateTransferUtil(radarjd, radarwd, radarheight);
    	for(int a = 0 ;a < 360 ; a++) {
    		double[] jwg = cTransfer.radar2Wgs84(jl, a, gd);
    		PGpoint point = new PGpoint(jwg[0], jwg[1]);
    		list.add(point);
    	}
    	
    	return list;
    
    }

    public List getRadarHeightPower1(int radarid,double radarjd,double radarwd,double radarheight,double maxdis
			,String tabname,double wlheight) throws Exception{
		// TODO Auto-generated method stub
		List<List<PGpoint>>  plist = new  ArrayList<List<PGpoint>>();
		CoordinateTransferUtil cTransfer = new CoordinateTransferUtil(radarjd, radarwd, radarheight);
		  LinkedHashMap<Integer,List<RayBean>> circleRayMap = new LinkedHashMap<Integer,List<RayBean>>();
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
			

			Statement stmt;
			List<PGpoint> testlist = new ArrayList<>();
			try {
				
				plist = getHeightLLPoint(radarjd, radarwd, radarheight, radarWL, wlheight);
				circleRayMap = getCircleMap(radarid, radarjd, radarwd, radarheight, maxdis, tabname, wlheight);
				PGpoint centerPoint = new PGpoint(radarjd, radarwd);
				 for(int key : circleRayMap.keySet()) {
					 List<RayBean> beanlist  = circleRayMap.get(key);
					 
					 for(List<PGpoint> blist : plist) {
						double tmpdis = getDistanceByGpoint(blist.get(key),centerPoint)*1000;
						RayBean bean = getBeanLeast(beanlist, centerPoint, wlheight, tmpdis);
						if(bean == null)
							break;
						double r = wlheight / Math.sin(Math.PI*bean.getYj()/180);
						double [] dd = cTransfer.radar2Wgs84(r, key,wlheight);
						double tydis = wlheight / Math.tan(Math.PI*bean.getYj()/180);
						if(tmpdis <= tydis) {
							break;
						}else {
							if(bean.getHeight()>=wlheight) {
								blist.get(key).x = bean.getPoint().x;
								blist.get(key).y = bean.getPoint().y;
							}else {
								blist.get(key).x = dd[0];
								blist.get(key).y = dd[1];
							}
						
						}
					
					 }
				     
				 }
				 
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			importPowerInto(plist, wlheight, radarid);
			return plist;
	}
    private void importPowerInto(List<List<PGpoint>> plist,double gdc,int radarid) throws SQLException {
    	String delsql = "delete from radarpower where radarid = "+radarid;
    	int rank = 1;
    	String vals = "";
    	for(List<PGpoint> list : plist) {
    		String val="( "+radarid+","+"0"+","+gdc+",'0',";
    		String jwds = "ST_Polygon(ST_GeomFromText('LINESTRING(";
    		for(PGpoint point : list) {
    			String jwd = point.x+" "+point.y+",";
    			jwds += jwd;
    		}
    		String lastjwd = list.get(0).x +" "+list.get(0).y;
    		jwds += lastjwd;
    		jwds += ")'),4326)";
    		val += jwds+","+rank+")";
    		vals += val+",";
    		rank++;
    	}
    	vals = vals.substring(0,vals.lastIndexOf(","));
    	String sql = "insert into radarpower(radarid,yj,gd,pointgd,wlgeom,rank) values "+ vals;
    	System.out.print(sql);
    	PgUtil.executeSql(delsql);
		PgUtil.executeSql(sql);
		PgUtil.destroy();
    }
    private RayBean getBeanLeast(List<RayBean> beanlist,PGpoint centerPoint,double wlheight,double maxdis) {
    	RayBean bean = beanlist.get(0);
    	for(RayBean rbean : beanlist) {
    		double sjdis = getDistanceByGpoint(bean.getPoint(),centerPoint)*1000;
			double tydis = wlheight / Math.tan(Math.PI * bean.getYj()/180);
			 double sjdis1 = getDistanceByGpoint(rbean.getPoint(),centerPoint)*1000;
			 double tydis1 = bean.getHeight()>=wlheight?sjdis1:wlheight / Math.tan(Math.PI * rbean.getYj()/180);
			 boolean isHigher = bean.getHeight()>=wlheight ? true : false;
			 if(sjdis < maxdis) {
				if(tydis1 < tydis) {
					bean = rbean;
				}
			 }
    	}
    	double jl = getDistanceByGpoint(bean.getPoint(),centerPoint)*1000;
    	if(jl> maxdis)
    		return null;
    	else
    	return bean;
    }
    public List getRadarHeightPower2(int radarid,double radarjd,double radarwd,double radarheight,double maxdis
			,String tabname,double wlheight) throws Exception{
		// TODO Auto-generated method stub
		List<List<PGpoint>>  plist = new  ArrayList<List<PGpoint>>();
		CoordinateTransferUtil cTransfer = new CoordinateTransferUtil(radarjd, radarwd, radarheight);
		  LinkedHashMap<Integer,RayBean> circleRayMap = new LinkedHashMap<Integer,RayBean>();
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
			

			Statement stmt;
			List<PGpoint> testlist = new ArrayList<>();
			try {
				PGpoint centerPoint = new PGpoint(radarjd, radarwd);
				plist = getHeightLLPoint(radarjd, radarwd, radarheight, radarWL, wlheight);
				double gddis = getDistanceByGpoint(plist.get(0).get(0),centerPoint)*1000;
				circleRayMap = getYJMaxCircleMap(radarid, radarjd, radarwd, radarheight, gddis, tabname, wlheight);
				 for(int key : circleRayMap.keySet()) {
					 RayBean bean  = circleRayMap.get(key);
					 for(List<PGpoint> blist : plist) {
						double tmpdis = getDistanceByGpoint(blist.get(key),centerPoint)*1000;
						if(bean == null)
							break;
						double r = wlheight / Math.sin(Math.PI*bean.getYj()/180);
						double [] dd = cTransfer.radar2Wgs84(r, key,wlheight);
						double tydis = wlheight / Math.tan(Math.PI*bean.getYj()/180);
						if(tmpdis <= tydis) {
							break;
						}else {
							if(bean.getHeight()>=wlheight) {
								blist.get(key).x = bean.getPoint().x;
								blist.get(key).y = bean.getPoint().y;
							}else {
								blist.get(key).x = dd[0];
								blist.get(key).y = dd[1];
							}
						
						}
					
					 }
				     
				 }
				 
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return plist;
	}
    private LinkedHashMap<Integer,RayBean> getYJMaxCircleMap(int radarid,double radarjd,double radarwd,double radarheight,double maxdis
			,String tabname,double wlheight){
		
		
		CoordinateTransferUtil cTransfer = new CoordinateTransferUtil(radarjd, radarwd, radarheight);
		  LinkedHashMap<Integer,RayBean> circleRayMap = new LinkedHashMap<Integer,RayBean>();
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
			
			/*String center = "ST_SetSRID(ST_MakePoint("+radarjd+" ,"+radarwd+"),4326)";
			String sql = " SELECT id,height,ST_distance("+center+"::geography,ST_Transform(point,4326)::geography) as dis , ST_AsText(point) as geom," + 
						"atand(height/ST_distance("+center+"::geography,st_transform(point,4326)::geography)) as yj ,"+
					    " row_number() over(partition by id order by atand(height/("+center+"::geography <-> point::geography)) desc) as ranking" + 
						" FROM " + 
						" (SELECT  r.id,  b.height, (ST_Dump(ST_Intersection(ST_Boundary(b.geom),r.geom))).geom AS point FROM " + 
						"  radarray r LEFT JOIN (select height,st_transform(geom,4326) as geom  from "+tabname+" where height >= "+radarheight+" and ST_DWithin(st_transform("+center+",32651),geom, "+maxdis+")) b on ST_Intersects(b.geom,r.geom)" + 
						" )g";
			*/
			String center = "ST_SetSRID(ST_MakePoint("+radarjd+" ,"+radarwd+"),4326)";
			String sql = " SELECT id,height,"+center+"::geography <-> ST_Transform(point,4326) as dis , ST_AsText(point) as geom," + 
						"atand(height/("+center+"::geography <-> st_transform(point,4326)::geography)) as yj ,"+
					    " row_number() over(partition by id order by atand(height/("+center+"::geography <-> point::geography)) desc ) as ranking" + 
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
					if(rank == 1) {
						circleRayMap.put(id, bean);
					}
				}
			}catch(Exception e) {
				
			}
			return circleRayMap;
	}
    @Override
    public List powerMerge(List<Integer> radarList,double heightlevel) {
    	/*String ids="(";
    	for(Integer radarid : radarList) {
    		ids += radarid+",";
    	}
    	ids = ids.substring(0,ids.lastIndexOf(","))+")";
    	String sql = "select wlgeom from  radarpower where radarid in "+ ids;
    	Statement stmt;
		try {
			stmt = PgUtil.getConn().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()) {
			//	String geom =  rs.getString(1);
				PGpolygon pp = (PGpolygon) rs.getObject(1);
			//	System.out.println(geom);
			}
		}catch(Exception e) {
			
		}
		return radarList;
    	*/
    	List list = new ArrayList<>();
    	String ids="(";
    	for(Integer radarid : radarList) {
    		ids += radarid+",";
    	}
    	ids = ids.substring(0,ids.lastIndexOf(","))+")";
    //	String sql = "SELECT ST_AsText(ST_SymDifference(ARRAY(SELECT wlgeom FROM radarpower where rank=1 and radarid in "+ids+"))) ";
    	String sql ="SELECT ST_AsText(ST_Union(ARRAY(SELECT ST_Accum(wlgeom) FROM radarpower)))";
    	//String sql = "select ST_AsText(wlgeom) from radarpower  ";
    //	String sql ="select ST_AsText(st_Union(ST_Accum((select wlgeom from radarPower  where radarid = 2 and rank =1 ),(select wlgeom from radarPower  where radarid = 1 and rank =1))))";
    	Statement stmt;
		try {
			stmt = PgUtil.getConn().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()) {
				String pointstr = rs.getString(1);
				List<String> strlist = extractMessageByRegular(pointstr);
				for(String str : strlist) {
					List tmplist = new ArrayList<>();
					String[] strA = str.split(",");
					for(String tmpstr : strA) {
						String[] tmA = tmpstr.split(" ");
						tmplist.add(tmA[0]);
						tmplist.add(tmA[1]);
						tmplist.add(heightlevel);
						
					}
					list.add(tmplist);
				}
			
			}
		    String mqSql = "select ST_AsText(wlgeom) from radarpower where rank = 2";
			ResultSet rs1 = stmt.executeQuery(mqSql);
			while(rs1.next()) {
				String pointstr = rs1.getString(1);
				List<String> strlist = extractMessageByRegular(pointstr);
				for(String str : strlist) {
					List tmplist = new ArrayList<>();
					String[] strA = str.split(",");
					for(String tmpstr : strA) {
						String[] tmA = tmpstr.split(" ");
						tmplist.add(tmA[0]);
						tmplist.add(tmA[1]);
						tmplist.add(heightlevel);
						
					}
					list.add(tmplist);
				}
			
			}
		
		}catch(Exception e) {
			
		}
		return list;
    	
    }
    @Override
    public  List powerMergeOf2Radar(Integer radarid1,Integer radarid2,double heightlevel) {
    	List list = new ArrayList<>();
    	int powerC1 = getPowerCountByRadarId(radarid1);
    	int powerC2 = getPowerCountByRadarId(radarid2);
    	//radar1、2最大圈交集
    	List flist = getFirstRankMergerListOf2Radar(radarid1, radarid2, heightlevel);
    	list.addAll(flist);
    	
    	//求radar2最大圈 与 radar1的交集
    	for(int rank = 2 ; rank <= powerC1; rank ++) {
    		boolean isIntersect = isIntersects(radarid1, rank, radarid2, 1, heightlevel);
    		List tmplist = new ArrayList<>();
    		if(isIntersect) {
    			tmplist = getDiff(radarid1, rank, radarid2, 1, heightlevel);
    		}else {
    			tmplist = getPList(radarid1, rank, heightlevel);
    		}
    		list.add(tmplist);
    	}
    	//求radar1最大圈 与 radar2的交集
    	for(int rank = 2 ; rank <= powerC2; rank ++) {
    		boolean isIntersect = isIntersects(radarid1, 1, radarid2, rank, heightlevel);
    		List tmplist = new ArrayList<>();
    		if(isIntersect) {
    			tmplist = getDiff(radarid2, rank, radarid1, 1, heightlevel);
    		}else {
    			tmplist = getPList(radarid2, rank, heightlevel);
    		}
    		list.add(tmplist);
    	}
		return list;
    	
    }
    //从radar1去除和radar2相交的部分后返回
    private List getDiff(Integer radarid1,Integer rank1,Integer radarid2,Integer rank2,double heightlevel) {
    	String sql = "select ST_AsText(ST_Difference("+
    			 "(select wlgeom from radarpower where radarid="+radarid1+" and rank = "+rank1+" and gd = "+heightlevel+"),"+
	             "(select wlgeom from radarpower where radarid="+radarid2+" and rank = "+rank2+" and gd = "+heightlevel+")))";
    	List tmplist = getGeomListBySql(sql, heightlevel);
    	return tmplist;
    	
    }
    private List getPList(Integer radarid,Integer rank,double heightlevel) {
    	String sql =  "select ST_AsText(wlgeom) from radarpower where radarid="+radarid+" and rank = "+rank+" and gd = "+heightlevel+"";
    	List tmplist = getGeomListBySql(sql, heightlevel);
		return tmplist;
    	
    }
    private boolean isIntersects(Integer radarid1,Integer rank1,Integer radarid2,Integer rank2,double heightlevel) {
		String sql = "select ST_Intersects("+
				     "(select wlgeom from radarpower where radarid="+radarid1+" and rank = "+rank1+" and gd = "+heightlevel+"),"+
		             "(select wlgeom from radarpower where radarid="+radarid2+" and rank = "+rank2+" and gd = "+heightlevel+") )";
		Statement stmt;
		boolean isIntersects = false;
		try {
			stmt = PgUtil.getConn().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()) {
				isIntersects = rs.getBoolean(1);
			}
		}catch(Exception e) {
			
		}
		
		return isIntersects;
    	
    }
    @Override
    public List getFirstRankMergerListOf2Radar(Integer radarid1,Integer radarid2,double heightlevel) {
    	String ids ="("+radarid1+","+radarid2+")";
    	List list = new ArrayList<>();
    	String sql = "SELECT ST_AsText(ST_Union(ARRAY(SELECT ST_Accum(wlgeom) FROM radarpower where radarid in "+ids+" and gd ="+heightlevel+" and rank = 1)))";
    	//String sql ="SELECT ST_AsText(ST_Union(ARRAY(SELECT ST_Accum(wlgeom) FROM radarpower)))";
    	try {
			Statement stmt = PgUtil.getConn().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()) {
				String pointstr = rs.getString(1);
				System.out.println(pointstr);
				List<String> strlist = extractMessageByRegular(pointstr);
				for(String str : strlist) {
					List tmplist = new ArrayList<>();
					String[] strA = str.split(",");
					for(String tmpstr : strA) {
						String[] tmA = tmpstr.split(" ");
						tmplist.add(tmA[0]);
						tmplist.add(tmA[1]);
						tmplist.add(heightlevel);
						
					}
					list.add(tmplist);
				}
			
			}
			rs.close();
			stmt.close();
    	}catch(Exception e) {
    		
    	}
    	//List tmplist = getGeomListBySql(sql, heightlevel);
		return list;
    	
    }
    private int getPowerCountByRadarId(int radarid) {
    	int powerCount = 0;
    	String sql = "select count(*) from radarpower where radarid = "+ radarid;
    	Statement stmt;
		try {
			stmt = PgUtil.getConn().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()) {
				powerCount = rs.getInt(1);
			}
		}catch(Exception e) {
			
		}
		return powerCount;
    }
    private List getGeomListBySql(String sql,double heightlevel) {
    	List tmplist = new ArrayList<>();
    	Statement stmt;
		try {
			stmt = PgUtil.getConn().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()) {
				String pointstr = rs.getString(1);
				List<String> strlist = extractMessageByRegular(pointstr);
				for(String str : strlist) {
					String[] strA = str.split(",");
					for(String tmpstr : strA) {
						String[] tmA = tmpstr.split(" ");
						tmplist.add(tmA[0]);
						tmplist.add(tmA[1]);
						tmplist.add(heightlevel);
						
					}
				}
			
			}
			rs.close();
			stmt.close();
		}catch(Exception e) {
			
		}
		return tmplist;
    	
    }
	/**
	 * 使用正则表达式提取中括号中的内容
	 * @param msg
	 * @return 
	 */
	private  static List<String> extractMessageByRegular(String msg){
		List<String> list = new ArrayList<String>();
		Pattern pattern = Pattern.compile("(?<=\\()[^\\)]+");  
	    //Pattern pattern = Pattern.compile("(\\()([0-9a-zA-Z\\.\\/\\=])*(\\))");
		Matcher matcher = pattern.matcher(msg);
		while(matcher.find()){
			 String mas = matcher.group().replaceAll("\\(", "");
			//  System.out.println(mas);
			  list.add(mas);
		}
		return list;
	}
	public static void main(String[] args) {
		String aa ="aa(((1,2)),((2,2)))";
		List<String> list = extractMessageByRegular(aa);
	}

	@Override
	public List loadHeightPower(Integer radarid, double heightlevel) {
		// TODO Auto-generated method stub
		String sql = "select ST_AsText(wlgeom) from radarpower where radarid ="+ radarid +" and gd ="+heightlevel;
		List list = new ArrayList<>();
    	Statement stmt;
		try {
			stmt = PgUtil.getConn().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()) {
				String pointstr = rs.getString(1);
				List tmplist = new ArrayList<>();				
				List<String> strlist = extractMessageByRegular(pointstr);
				for(String str : strlist) {
					String[] strA = str.split(",");
					for(String tmpstr : strA) {
						String[] tmA = tmpstr.split(" ");
						tmplist.add(tmA[0]);
						tmplist.add(tmA[1]);
						tmplist.add(heightlevel);
						
					}
				}
				list.add(tmplist);
			
			}
			rs.close();
			stmt.close();
		}catch(Exception e) {
			
		}
		return list;
	}
}
