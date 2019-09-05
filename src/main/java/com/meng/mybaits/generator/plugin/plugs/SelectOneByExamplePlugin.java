package com.meng.mybaits.generator.plugin.plugs;

import java.util.List;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 * 条件查询返回单条,多条会抛出异常
 * 
 * @author ChenMeng
 * @time 2019年9月4日
 */
public class SelectOneByExamplePlugin extends PluginAdapter{
	
	/**
	 * 查询单条
	 */
	public static final String METHOD_SELECT_ONE_BY_EXAMPLE = "selectOneByExample";

	@Override
	public boolean validate(List<String> warnings) {
		String driverClass = getContext().getJdbcConnectionConfiguration().getDriverClass();
		String targetRuntime = getContext().getTargetRuntime();
		if(!"MyBatis3".equals(targetRuntime)) {
			System.out.println("SelectOneByExamplePlugin要求运行targetRuntime必须为MyBatis3!");
			return false;
		}
		if(!("oracle.jdbc.driver.OracleDriver".equals(driverClass)
				|| "com.mysql.jdbc.Driver".equals(driverClass)
				|| "com.mysql.cj.jdbc.Driver".equals(driverClass))) {
			System.out.println("SelectOneByExamplePlugin只支持oracle和mysql!");
			return false;
		}
		return true;
	}
	
	@Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        // 创建方法
        Method method = new Method(METHOD_SELECT_ONE_BY_EXAMPLE);
        
        // 添加参数
        FullyQualifiedJavaType paramType = new FullyQualifiedJavaType(introspectedTable.getExampleType());
        method.addParameter(new Parameter(paramType, "example"));
        // 添加返回值类型
        FullyQualifiedJavaType returnType = introspectedTable.getRules().calculateAllFieldsClass();
        method.setReturnType(returnType);
        
        interfaze.addMethod(method);
        return true;
	}
	
	/**
	 * xml生成
	 */
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		// 表名
		String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
		XmlElement selectOneByExampleElt = new XmlElement("select");
		selectOneByExampleElt.addAttribute(new Attribute("id", METHOD_SELECT_ONE_BY_EXAMPLE));// 方法名
		selectOneByExampleElt.addAttribute(new Attribute("parameterType", introspectedTable.getExampleType()));// 参数类型
		selectOneByExampleElt.addAttribute(new Attribute("resultMap", "BaseResultMap"));// 参数类型
 		
        XmlElement ifElment = new XmlElement("if");
        ifElment.addAttribute(new Attribute("test", "distinct"));
        ifElment.addElement(new TextElement("distinct"));
        
        XmlElement includeElment = new XmlElement("include");
        includeElment.addAttribute(new Attribute("refid", "Base_Column_List"));
        
        XmlElement includeElment01 = new XmlElement("include");
        includeElment01.addAttribute(new Attribute("refid", "Example_Where_Clause"));
        
        XmlElement ifElment01 = new XmlElement("if");
        ifElment01.addAttribute(new Attribute("test", "_parameter != null"));
        ifElment01.addElement(includeElment01);
        
        XmlElement ifElment02 = new XmlElement("if");
        ifElment02.addAttribute(new Attribute("test", "orderByClause != null"));
        ifElment02.addElement(new TextElement("order by ${orderByClause}"));
		
		selectOneByExampleElt.addElement(new TextElement("select"));
		selectOneByExampleElt.addElement(ifElment);
		selectOneByExampleElt.addElement(includeElment);
		selectOneByExampleElt.addElement(new TextElement("from " + tableName));
		selectOneByExampleElt.addElement(ifElment01);
		selectOneByExampleElt.addElement(ifElment02);
		
        if (context.getPlugins().sqlMapInsertElementGenerated(selectOneByExampleElt, introspectedTable)) {
            document.getRootElement().addElement(selectOneByExampleElt);
        }
		return true;
	}

}
