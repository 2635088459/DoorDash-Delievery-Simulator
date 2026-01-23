package com.shydelivery.doordashsimulator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 天气服务 - Phase 2
 * 
 * 用于判断当前天气状况，影响配送费计算
 * 
 * 当前实现: 模拟天气服务
 * 未来可以集成真实天气 API，例如：
 * - OpenWeatherMap API
 * - WeatherAPI
 * - AccuWeather API
 */
@Slf4j
@Service
public class WeatherService {
    
    /**
     * 检查指定位置当前是否为恶劣天气
     * 
     * 恶劣天气包括:
     * - 大雨/暴雨
     * - 大雪/暴雪
     * - 雷暴
     * - 大风
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @param time 时间
     * @return true 如果是恶劣天气，false 如果天气正常
     */
    public boolean isBadWeather(BigDecimal latitude, BigDecimal longitude, LocalDateTime time) {
        // TODO: 集成真实天气 API
        // 示例集成代码:
        /*
        String apiKey = "YOUR_API_KEY";
        String url = String.format(
            "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s",
            latitude, longitude, apiKey
        );
        
        // 调用 API 获取天气数据
        WeatherResponse response = restTemplate.getForObject(url, WeatherResponse.class);
        
        // 判断天气条件
        if (response != null && response.getWeather() != null) {
            String condition = response.getWeather().get(0).getMain();
            return "Rain".equals(condition) || "Snow".equals(condition) || 
                   "Thunderstorm".equals(condition);
        }
        */
        
        // 当前模拟实现：随机模拟恶劣天气（15% 概率）
        // 实际部署时应该替换为真实 API 调用
        boolean badWeather = Math.random() < 0.15;
        
        if (badWeather) {
            log.info("模拟天气服务: 检测到恶劣天气 (位置: {}, {}, 时间: {})", 
                latitude, longitude, time);
        }
        
        return badWeather;
    }
    
    /**
     * 获取天气描述（用于前端显示）
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @return 天气描述字符串
     */
    public String getWeatherDescription(BigDecimal latitude, BigDecimal longitude) {
        // TODO: 从天气 API 获取实际天气描述
        
        if (isBadWeather(latitude, longitude, LocalDateTime.now())) {
            String[] badWeatherTypes = {"大雨", "暴雨", "大雪", "雷暴", "大风"};
            int index = (int) (Math.random() * badWeatherTypes.length);
            return badWeatherTypes[index];
        }
        
        String[] normalWeatherTypes = {"晴", "多云", "阴", "小雨"};
        int index = (int) (Math.random() * normalWeatherTypes.length);
        return normalWeatherTypes[index];
    }
    
    /**
     * 获取温度（摄氏度）
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @return 温度值
     */
    public Double getTemperature(BigDecimal latitude, BigDecimal longitude) {
        // TODO: 从天气 API 获取实际温度
        
        // 模拟温度：15-30度
        return 15.0 + (Math.random() * 15.0);
    }
}
