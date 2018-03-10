package com.TZONE.Bluetooth.Utils;

/**
 * Temperature unit conversion
 * Created by Forrest on 2017/4/14.
 */
public class TemperatureUnitUtil {

    private final int number = 1;
    /**
     * Degrees Celsius
     */
    private double _Temperature;

    public TemperatureUnitUtil(){
        _Temperature = -1000;
    }
    public TemperatureUnitUtil(double celsius){
        _Temperature = celsius;
    }

    /**
     * Degrees Celsius
     * @return
     */
    public double GetCelsius(){
        return Double.parseDouble(StringUtil.ToString(_Temperature,number));
    }
    public String GetStringCelsius(){
        return StringUtil.ToString(_Temperature,number) + "℃";
    }

    /**
     * Fahrenheit
     * @return
     */
    public double GetFahrenheit(){
        return Double.parseDouble(StringUtil.ToString((_Temperature * 1.8 + 32),number));
    }
    public String GetStringFahrenheit(){
        return StringUtil.ToString((_Temperature * 1.8 + 32),number)  +"℉";
    }
    public void SetFahrenheit(double res){
        _Temperature = (res - 32) / 1.8;
    }

    /**
     * Kelvin
     * @return
     */
    public double GetKelvin(){
        return Double.parseDouble(StringUtil.ToString((_Temperature + 273.15),number));
    }
    public String GetStringKelvin(){
        return StringUtil.ToString((_Temperature + 273.15),number) + "K";
    }
    public void SetKelvin(double res){
        _Temperature = res - 273.15;
    }

    /**
     * Rankine
     * @return
     */
    public double GetRankine(){
        return Double.parseDouble(StringUtil.ToString((_Temperature * 1.8 + 32 + 459.67),number));
    }
    public String GetStringRankine(){
        return StringUtil.ToString((_Temperature * 1.8 + 32 + 459.67),number)  + "°R";
    }
    public void SetRankine(double res){
        _Temperature = (res - 459.67 - 32)/1.8;
    }

    /**
     * Reaumur
     * @return
     */
    public double GetReaumur(){
        return Double.parseDouble(StringUtil.ToString((_Temperature * 0.8),number));
    }
    public String GetStringReaumur(){
        return StringUtil.ToString((_Temperature * 0.8),number) + "°Re";
    }
    public void SetReaumur(double res){
        _Temperature = res / 0.8;
    }

    public double GetTemperature(int unit){
        switch (unit){
            case 1:return GetFahrenheit();
            case 2:return GetKelvin();
            case 3:return GetRankine();
            case 4:return GetReaumur();
            default:return GetCelsius();
        }
    }
    public double GetTemperature(int unit,double res){
        switch (unit){
            case 1:SetFahrenheit(res);break;
            case 2:SetKelvin(res);break;
            case 3:SetRankine(res);break;
            case 4:SetReaumur(res);break;
            default:_Temperature = res;break;
        }
        return GetCelsius();
    }
    public String GetStringTemperature(int unit){
        switch (unit){
            case 1:return GetStringFahrenheit();
            case 2:return GetStringKelvin();
            case 3:return GetStringRankine();
            case 4:return GetStringReaumur();
            default:return GetStringCelsius();
        }
    }
}
