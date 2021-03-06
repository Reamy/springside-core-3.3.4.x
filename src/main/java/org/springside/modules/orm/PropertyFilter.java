/**
 * Copyright (c) 2005-20010 springside.org.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * 
 * $Id: PropertyFilter.java 1205 2010-09-09 15:12:17Z calvinxiu $
 */
package org.springside.modules.orm;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springside.modules.utils.reflection.ConvertUtils;
import org.springside.modules.utils.web.ServletUtils;

/**
 * 与具体ORM实现无关的属性过滤条件封装类, 主要记录页面中简单的搜索过滤条件.
 * 
 * @author calvin
 */
public class PropertyFilter {

	/** 多个属性间OR关系的分隔符. */
	public static final String OR_SEPARATOR = "_OR_";

	/** 属性比较类型. 
	 * 
	 * NN:not null
	 * IN: is null 
	 */
	public enum MatchType {
		EQ, NE, LIKE, LLIKE, RLIKE, LT, GT, LE, GE, NN, IN, BTD;
	}

	/** 属性数据类型. */
	public enum PropertyType {
		S(String.class), I(Integer.class), L(Long.class), F(Float.class), N(Double.class),
        D(Date.class), B(Boolean.class), W(String.class);

		private Class<?> clazz;

		private PropertyType(Class<?> clazz) {
			this.clazz = clazz;
		}

		public Class<?> getValue() {
			return clazz;
		}
	}

	private MatchType matchType = null;
	private Object matchValue = null;
	private String originValue = null;
    private String propertyTypeCode = null;

	private Class<?> propertyClass = null;
	private String[] propertyNames = null;

	public PropertyFilter() {
	}

	/**
	 * @param filterName 比较属性字符串,含待比较的比较类型、属性值类型及属性列表. 
	 *                   eg. LIKES_NAME_OR_LOGIN_NAME
	 * @param value 待比较的值.
	 */
	public PropertyFilter(final String filterName, final String value) {

		String firstPart = StringUtils.substringBefore(filterName, "_");
		String matchTypeCode = StringUtils.substring(firstPart, 0, firstPart.length() - 1);
		this.propertyTypeCode = StringUtils.substring(firstPart, firstPart.length() - 1, firstPart.length());

		try {
			matchType = Enum.valueOf(MatchType.class, matchTypeCode);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("filter名称" + filterName + "没有按规则编写,无法得到属性比较类型.", e);
		}

		try {
			propertyClass = Enum.valueOf(PropertyType.class, propertyTypeCode).getValue();
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("filter名称" + filterName + "没有按规则编写,无法得到属性值类型.", e);
		}

		String propertyNameStr = StringUtils.substringAfter(filterName, "_");
		Assert.isTrue(StringUtils.isNotBlank(propertyNameStr), "filter名称" + filterName + "没有按规则编写,无法得到属性名称.");
		//		propertyNames = StringUtils.splitByWholeSeparator(propertyNameStr, PropertyFilter.OR_SEPARATOR);
		propertyNames = propertyNameStr.split(PropertyFilter.OR_SEPARATOR);

		this.originValue = value;

		if (matchType.equals(MatchType.IN) || matchType.equals(MatchType.NN)) {
			this.matchValue = null;
		} else {
			this.matchValue = ConvertUtils.convertStringToObject(value, propertyClass);
		}
	}

	/**
	 * 从HttpRequest中创建PropertyFilter列表, 默认Filter属性名前缀为filter.
	 * 
	 * @see #buildFromHttpRequest(HttpServletRequest, String)
	 */
	public static List<PropertyFilter> buildFromHttpRequest(final HttpServletRequest request) {
		return buildFromHttpRequest(request, "filter");
	}

	/**
	 * 从HttpRequest中创建PropertyFilter列表
	 * PropertyFilter命名规则为Filter属性前缀_比较类型属性类型_属性名.
	 * 
	 * eg.
	 * filter_EQS_name
	 * filter_LIKES_name_OR_email
	 */
	public static List<PropertyFilter> buildFromHttpRequest(final HttpServletRequest request, final String filterPrefix) {
		List<PropertyFilter> filterList = new ArrayList<PropertyFilter>();

		//从request中获取含属性前缀名的参数,构造去除前缀名后的参数Map.
		Map<String, Object> filterParamMap = ServletUtils.getParametersStartingWith(request, filterPrefix + "_");

		//分析参数Map,构造PropertyFilter列表
		for (Map.Entry<String, Object> entry : filterParamMap.entrySet()) {
			String filterName = entry.getKey();
			String value = null;
			try {
				String tempValue = entry.getValue() == null ? "" : entry.getValue().toString();
				value = URLDecoder.decode(tempValue, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

            String firstPart = StringUtils.substringBefore(filterName, "_");
            String matchTypeCode = StringUtils.substring(firstPart, 0, firstPart.length() - 1);

			//如果value值为空,则忽略此filter.
			if (matchTypeCode.equals("IN") || StringUtils.isNotBlank(value)) {
				PropertyFilter filter = new PropertyFilter(filterName, value);
				filterList.add(filter);
			}
		}

		return filterList;
	}

	/**
	 * 获取比较值的类型.
	 */
	public Class<?> getPropertyClass() {
		return propertyClass;
	}

	/**
	 * 获取比较方式.
	 */
	public MatchType getMatchType() {
		return matchType;
	}

	/**
	 * 获取比较值.
	 */
	public Object getMatchValue() {
		return matchValue;
	}

	/**
	 * 获取原始值
	 * @return
	 */
	public String getOriginValue() {
		return originValue;
	}

	/**
	 * 获取比较属性名称列表.
	 */
	public String[] getPropertyNames() {
		return propertyNames;
	}

	/**
	 * 获取唯一的比较属性名称.
	 */
	public String getPropertyName() {
		Assert.isTrue(propertyNames.length == 1, "There are not only one property in this filter.");
		return propertyNames[0];
	}

	/**
	 * 是否比较多个属性.
	 */
	public boolean hasMultiProperties() {
		return (propertyNames.length > 1);
	}

	/**
	 * 根据MatchType获取sql比较符号
	 */
	public String getSqlOper() {
		Map<PropertyFilter.MatchType, String> types = new HashMap<PropertyFilter.MatchType, String>();
		types.put(MatchType.EQ, "=");
		types.put(MatchType.NE, "!=");
		types.put(MatchType.GT, ">");
		types.put(MatchType.LT, "<");
		types.put(MatchType.GE, ">=");
		types.put(MatchType.LE, "<=");
		types.put(MatchType.IN, "is null");
		types.put(MatchType.NN, "is not null");
		return types.get(this.getMatchType());
	}

	public void setMatchType(MatchType matchType) {
		this.matchType = matchType;
	}

	public void setMatchValue(Object matchValue) {
		this.matchValue = matchValue;
	}

	public void setOriginValue(String originValue) {
		this.originValue = originValue;
	}

	public void setPropertyClass(Class<?> propertyClass) {
		this.propertyClass = propertyClass;
	}

	public void setPropertyNames(String[] propertyNames) {
		this.propertyNames = propertyNames;
	}

    public String getPropertyTypeCode() {
        return propertyTypeCode;
    }

    public void setPropertyTypeCode(String propertyTypeCode) {
        this.propertyTypeCode = propertyTypeCode;
    }
}
