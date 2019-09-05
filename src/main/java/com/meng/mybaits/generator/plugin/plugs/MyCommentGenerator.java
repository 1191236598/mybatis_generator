package com.meng.mybaits.generator.plugin.plugs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.internal.DefaultCommentGenerator;

import com.mysql.jdbc.StringUtils;

/**
 * 读取数据库注释生成器
 * (只有类注释和属性注释,setter和getter方法注释留着位置,自己添加)
 * @author ChenMeng
 * @time 2019年8月30日
 */
public class MyCommentGenerator extends DefaultCommentGenerator {
	
	//自定义属性
	private Properties properties;
	
	@Override
	public void addConfigurationProperties(Properties properties) {
		// 获取自定义的 properties
	    this.properties = properties;
	    // 保持原来的属性功能
	    super.addConfigurationProperties(properties);
	}
	
    @Override
    public void addFieldComment(Field field, IntrospectedTable introspectedTable,
    		IntrospectedColumn introspectedColumn) {
        // 获取列注释
        String remarks = introspectedColumn.getRemarks();
        field.addJavaDocLine("/**");
        field.addJavaDocLine(" * " + remarks);
        field.addJavaDocLine(" */");
    }
    
    @Override
    public void addModelClassComment(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
      String author = properties.getProperty("author", "");
      String dateFormat = properties.getProperty("dateFormat", "yyyy-MM-dd");
      SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
   
      // 获取表注释
      String remarks = introspectedTable.getRemarks();
      
      remarks = StringUtils.isNullOrEmpty(remarks)?introspectedTable.getTableConfiguration().toString():remarks;
      topLevelClass.addJavaDocLine("/**");
      topLevelClass.addJavaDocLine(" * " + remarks);
      topLevelClass.addJavaDocLine(" *");
      if(!StringUtils.isNullOrEmpty(author)) topLevelClass.addJavaDocLine(" * @author " + author);
      topLevelClass.addJavaDocLine(" * @date " + dateFormatter.format(new Date()));
      topLevelClass.addJavaDocLine(" */");
      
    }

    @Override
    public void addSetterComment(Method method, IntrospectedTable introspectedTable,
    		IntrospectedColumn introspectedColumn) {
    	//TODO set方法上面注释
    }
    
    @Override
    public void addGetterComment(Method method, IntrospectedTable introspectedTable,
    		IntrospectedColumn introspectedColumn) {
    	//TODO get方法上面注释
    }
}
