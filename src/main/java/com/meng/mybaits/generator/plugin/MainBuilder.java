package com.meng.mybaits.generator.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;

/**
 * 自动构建工具类
 * @author ChenMeng
 *
 */
public class MainBuilder {

    public static void main(String[] args) throws Exception {
        builder();
    }
    
	public static void builder() throws Exception{
		List<String> warnings = new ArrayList<String>();
		// 指定 逆向工程配置文件
		File configFile = new File(Class.class.getResource("/BuilderConfig.xml").getPath());
		ConfigurationParser cp = new ConfigurationParser(warnings);
        try {
            Configuration config = cp.parseConfiguration(configFile);
            DefaultShellCallback callback = new DefaultShellCallback(true);
            MyBatisGenerator myBatisGenerator = new MyBatisGenerator(config, callback, warnings);
            myBatisGenerator.generate(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
