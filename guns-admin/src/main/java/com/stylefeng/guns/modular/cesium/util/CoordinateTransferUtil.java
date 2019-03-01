package com.stylefeng.guns.modular.cesium.util;

public class CoordinateTransferUtil {
    private double EarthRadius = 6378137.0;
    private double e2 = 0.00669437999013;
    private double radarlon, radarlat, radarheight;
    private double maxHdiff;//最大高度误差阈值
    private double X0 = 0, Y0 = 0, Z0 = 0;
    double radarN;//卯酉圆曲率半径
    private int iterCount = 40;///迭代法求纬度的迭代次数。 
    private double sinradarlon, cosradarlon, sinradarlat, cosradarlat;
    public CoordinateTransferUtil() {
    	
    }
    public CoordinateTransferUtil(double a, double b, double c) {
    	 this.radarlon = a;
         this.radarlat = b;
         this.radarheight = c;

         this.sinradarlon = Math.sin(Math.PI * radarlon / 180);
         this.cosradarlon = Math.cos(Math.PI * radarlon / 180);
         this.sinradarlat = Math.sin(Math.PI * radarlat / 180);
         this.cosradarlat = Math.cos(Math.PI * radarlat / 180);
         this.radarN = EarthRadius / (Math.sqrt(1 - e2 * Math.pow(sinradarlat, 2)));

         /// 雷达坐标转换为大地直角坐标 
         this.X0 = (radarN + radarheight) * cosradarlat * cosradarlon;
         this.Y0 = (radarN + radarheight) * cosradarlat * sinradarlon;
         this.Z0 = (radarN * (1 - e2) + radarheight) * sinradarlat;
    }
    /// <summary>
    /// gps的WGS-84经纬度以及高度转换为以radar为中心的极坐标
    /// 
    ///方法：1、将gps坐标、radar坐标转换为大地直角坐标
    ///      2、将gps的大地直角坐标转换为以radar为坐标原点的的enu（东北天）坐标//站心直角坐标
    ///      3、将gps相对radar的的enu坐标转换为极坐标。   //站心极坐标
    /// </summary>
    /// <param name="lon">目标经度</param>
    /// <param name="lat">目标纬度</param>
    /// <param name="height">目标海拔高度</param>
    /// <param name="jzb[0]">斜距</param>
    /// <param name="jzb[1]">方位角</param>
    /// <param name="jzb[2]">俯仰角</param>
    
    public double[] wgs842polar(double lon, double lat, double height)
    {
    	double[] jzb = new double[3];
    	double r,a,b;
        double sinlon = Math.sin(Math.PI * lon / 180);//经度sin 参数
        double coslon = Math.cos(Math.PI * lon / 180);
        double sinlat = Math.sin(Math.PI * lat / 180);//维度sin参数
        double coslat = Math.cos(Math.PI * lat / 180);
        double N = EarthRadius / (Math.sqrt(1 - e2 * Math.pow(sinlat, 2)));//卯酉圆曲率半径

        ///1、gps 转换为大地直角坐标
        double X = (N + height) * coslat * coslon;
        double Y = (N + height) * coslat * sinlon;
        double Z = (N * (1 - e2) + height) * sinlat;


        ///2、大地直角坐标转换为enu坐标 即站心直角坐标
        double x = -sinradarlon * (X - X0) + cosradarlon * (Y - Y0);
        double y = -sinradarlat * cosradarlon * (X - X0) - sinradarlon * sinradarlat * (Y - Y0) + cosradarlat * (Z - Z0);
        double z = cosradarlat * cosradarlon * (X - X0) + cosradarlat * sinradarlon * (Y - Y0) + sinradarlat * (Z - Z0);

        ///3、站心直角坐标转换为站心极坐标。
        jzb[0]= Math.sqrt(x * x + y * y + z * z);
        jzb[1]= Math.atan(x / y) * 180 / Math.PI;
        jzb[2]= Math.asin(z / jzb[0]) * 180 / Math.PI;
        return jzb;
    }
    
    public double[] polar2WGS84(double r, double a, double b)
    {
    	double[] jwgd= new double[3];
    	double lon,lat,height;
        ///1、站心极坐标转为站心直角坐标。
        double x1 = r * Math.cos(Math.PI * b / 180) * Math.sin(Math.PI * a / 180);
        double y1 = r * Math.cos(Math.PI * b / 180) * Math.cos(Math.PI * a / 180);
        double z1 = r * Math.sin(Math.PI * b / 180);
        ///2、站心直角坐标系 转换为 大地直角坐标系
        double xe = -sinradarlon * x1 - sinradarlat * cosradarlon * y1 + cosradarlon * cosradarlat * z1 + X0;
        double yn = cosradarlon * x1 - sinradarlat * sinradarlon * y1 + cosradarlat * sinradarlon * z1 + Y0;
        double zu = cosradarlat * y1 + sinradarlat * z1 + Z0;
        ///3、大地直角坐标转换为经纬度WGS-84

        lon = Math.atan(yn / xe) * 180 / Math.PI;
        if (lon < 0)
            lon = 180 + lon;
        double initLat = Math.atan(zu / (Math.sqrt(xe * xe + yn * yn))) * 180 / Math.PI;
        lat = latIterate(initLat, xe, yn, zu);
        height = Math.sqrt(xe * xe + yn * yn) / Math.cos(Math.PI * lat / 180) - radarN;
        jwgd[0]= lon;
        jwgd[1]= lat;
        jwgd[2]= height;
		return jwgd;

    }
    private double latIterate(double initLat, double x, double y, double z)
    {
    	  
    	double finalLat = initLat;
        int i = 0;
        while (true)
        {
            if (i >= iterCount)
            {
                break;
            }
            double sinlat = Math.sin(Math.PI * finalLat / 180);
            double N = EarthRadius / (Math.sqrt(1 - e2 * Math.pow(sinlat, 2)));
            finalLat = Math.atan((z + N * e2 * sinlat) / Math.sqrt(x * x + y * y)) * 180 / Math.PI;
            i++;
        }
        return finalLat;
    }

    /// <summary>
    ///  gps的WGS经纬度以及高度转换为以雷达为中心的坐标(斜距，方位角，高度)
    /// </summary>
    /// <param name="lon"></param>
    /// <param name="lat"></param>
    /// <param name="height"></param>
    /// <param name="r">斜距</param>
    /// <param name="a">方位角</param>
    /// <param name="h">高度</param>
    public double[] wgs842Radar(double lon, double lat, double height)
    {
    	double[] jzb = new double[3];
    	double r,a,h;
        double sinlon = Math.sin(Math.PI * lon / 180);//经度sin 参数
        double coslon = Math.cos(Math.PI * lon / 180);
        double sinlat = Math.sin(Math.PI * lat / 180);//维度sin参数
        double coslat = Math.cos(Math.PI * lat / 180);
        double N = EarthRadius / (Math.sqrt(1 - e2 * Math.pow(sinlat, 2)));//卯酉圆曲率半径

        ///1、gps 转换为大地直角坐标
        double X = (N + height) * coslat * coslon;
        double Y = (N + height) * coslat * sinlon;
        double Z = (N * (1 - e2) + height) * sinlat;


        ///2、大地直角坐标转换为enu坐标 即站心直角坐标
        double x = -sinradarlon * (X - X0) + cosradarlon * (Y - Y0);
        double y = -sinradarlat * cosradarlon * (X - X0) - sinradarlon * sinradarlat * (Y - Y0) + cosradarlat * (Z - Z0);
        double z = cosradarlat * cosradarlon * (X - X0) + cosradarlat * sinradarlon * (Y - Y0) + sinradarlat * (Z - Z0);

        ///3、站心直角坐标转换为站心极坐标。
        r = Math.sqrt(x * x + y * y + z * z);
        a = Math.atan(x / y) * 180 / Math.PI;
        h = z;
        jzb[0] = r;
        jzb[1] = a;
        jzb[2] = h;
        return jzb;


    }
    /// <summary>
    /// 雷达为中心的坐标(斜距，方位角，高度)转换成经纬度高度
    /// </summary>
    /// <param name="r"></param>
    /// <param name="a"></param>
    /// <param name="h"></param>
    /// <param name="lon"></param>
    /// <param name="lat"></param>
    /// <param name="height"></param>
    public double[] radar2Wgs84(double r, double a, double h)
    {
    	double[] jwg = new double[3];
    	 double lon,  lat,  height;
        ///1、站心极坐标转为站心直角坐标。
        double xy = Math.sqrt(r * r - h * h);
        double x1 = xy * Math.sin(Math.PI * a / 180);
        double y1 = xy * Math.cos(Math.PI * a / 180);
        double z1 = h;
        ///2、站心直角坐标系 转换为 大地直角坐标系
        double xe = -sinradarlon * x1 - sinradarlat * cosradarlon * y1 + cosradarlon * cosradarlat * z1 + X0;
        double yn = cosradarlon * x1 - sinradarlat * sinradarlon * y1 + cosradarlat * sinradarlon * z1 + Y0;
        double zu = cosradarlat * y1 + sinradarlat * z1 + Z0;
        ///3、大地直角坐标转换为经纬度WGS-84

        lon = Math.atan(yn / xe) * 180 / Math.PI;
        if (lon < 0)
            lon = 180 + lon;
        double initLat = Math.atan(zu / (Math.sqrt(xe * xe + yn * yn))) * 180 / Math.PI;
        lat = latIterate(initLat, xe, yn, zu);
        height = Math.sqrt(xe * xe + yn * yn) / Math.cos(Math.PI * lat / 180) - radarN;
        jwg[0] = lon;
        jwg[1] = lat;
        jwg[2] = height;
        return jwg;
    }
    
    /// <summary>
    ///测距 
    /// </summary>
    /// <param name="lng1">点1经度</param>
    /// <param name="lat1">点1纬度</param>
    /// <param name="lng2">点2经度</param>
    /// <param name="lat2">点2纬度</param>
    /// <returns>km</returns>
    public static double distance(double lng1, double lat1, double lng2, double lat2)
    {
        double latRadians1 = lat1 * (Math.PI / 180);
        double latRadians2 = lat2 * (Math.PI / 180);
        double latRadians = latRadians1 - latRadians2;
        double lngRadians = lng1 * (Math.PI / 180) - lng2 * (Math.PI / 180);
        double f = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(latRadians / 2), 2) + Math.cos(latRadians1) * Math.cos(latRadians2) * Math.pow(Math.sin(lngRadians / 2), 2)));
        return f * 6378.137;
    }
    public static void main(String[] args) {
    	System.out.println(Math.sin(90*Math.PI/180));
    }
}
