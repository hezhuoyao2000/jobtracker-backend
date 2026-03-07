package com.example.myfirstspringboot.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * PostgreSQL 数组类型处理器
 * 用于在 Java List 和 PostgreSQL text[] 之间转换
 */
public class StringArrayTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        // 构建 PostgreSQL 数组格式：{"elem1","elem2"}
        StringBuilder sb = new StringBuilder("{");
        for (int idx = 0; idx < parameter.size(); idx++) {
            if (idx > 0) sb.append(",");
            sb.append("\"").append(parameter.get(idx).replaceAll("\"", "\\\\\"")).append("\"");
        }
        sb.append("}");
        ps.setString(i, sb.toString());
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseArray(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseArray(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseArray(cs.getString(columnIndex));
    }

    private List<String> parseArray(String arrayStr) throws SQLException {
        if (arrayStr == null || arrayStr.isEmpty()) {
            return null;
        }
        // PostgreSQL 数组格式：{"elem1","elem2"}
        // 移除花括号
        arrayStr = arrayStr.replaceAll("^\\{|\\}$", "");
        if (arrayStr.isEmpty()) {
            return List.of();
        }
        // 分割并清理引号
        String[] elements = arrayStr.split(",");
        List<String> result = new java.util.ArrayList<>();
        for (String elem : elements) {
            result.add(elem.trim().replaceAll("^\"|\"$", ""));
        }
        return result;
    }
}
