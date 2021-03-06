package com.kuangcp.mythpoi.utils.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 切记要导JAR驱动包，还有配置好url
 *
 * @author Myth on 2016年7月24日
 * TODO 配置文件的外化 YML
 * TODO 重写整个数据库模块, 回归JDBC初衷
 */

public class Mysql {

  private static int count = 0;
  private PreparedStatement ps = null;
  private Connection cn = null;
  private ResultSet rs = null;
  private String driver;
  private StringBuilder url = new StringBuilder();

  /**
   * 手动设置链接数据的属性
   */
  public Mysql(BaseConfig config) {
    this.driver = config.getDriver();
    this.url.append("jdbc:mysql://").append(config.getHost()).append(":").append(config.getPort())
        .append("/").append(config.getDatabase()).append("?user=")
        .append(config.getUsername()).append("&password=").append(config.getPassword())
        .append("&userUnicode=true&characterEncoding=UTF8");
  }

  /**
   * 采用默认配置文件作为默认配置
   */
  public Mysql() {
    this(BaseConfig.initByYaml());
  }

  /**
   * 根据参数获取数据库连接对象
   *
   * @return Connection 连接
   */
  public Connection getConnection() {
    try {
      Class.forName(driver);
      cn = DriverManager.getConnection(url.toString());
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("获取连接，异常！");
    }
    return cn;
  }

  private void loadPreparedStatement(String sql) throws SQLException {
    getConnection();
    ps = cn.prepareStatement(sql);
  }

  /**
   * 查询全部的操作 返回值是ResultSet 切记使用完后要finally关闭
   */
  public ResultSet queryBySQL(String sql) {
    count++;
    try {
      loadPreparedStatement(sql);
      rs = ps.executeQuery();
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("这是第" + count + "次查询操作");
    return rs;
  }

  /**
   * SQL查询并返回List集合
   *
   * @param sql SQL 语句
   * @return List String数组 一行是一个String[] 按查询的字段顺序
   */
  public List<String[]> queryReturnList(String sql) {
    int cols;
    List<String[]> data = new ArrayList<>(0);
    ResultSet rs = queryBySQL(sql);
    try {
      //获取总列数
      cols = rs.getMetaData().getColumnCount();
      while (rs.next()) {
        //为什么放在while外面就会出现最后一组元素覆盖整个数组？
        String[] row = new String[cols];
        for (int i = 0; i < cols; i++) {
          row[i] = rs.getString(++i);
        }
        data.add(row);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      this.closeAll();
    }
    return data;
  }

  /**
   * 把增删改 合在一起 返回值是 布尔值
   * 各种连接已经关闭了不用再次关闭了
   * SQL只能输一句，不能多句运行
   *
   * @param sql 执行的SQL
   * @return boolean 是否执行成功
   */
  public boolean executeUpdateSQL(String sql) {
    boolean flag = true;
    try {
      loadPreparedStatement(sql);
      int i = ps.executeUpdate();
      System.out.print("    增删改查成功_" + i + "_行受影响-->");
      if (i != 1) {
        flag = false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("增删改查失败");
      flag = false;
    } finally {
      this.closeAll();
    }
    return flag;
  }

  /**
   * 插入多条数据并采用了事务
   *
   * @param sqls SQL的String数组
   * @return boolean 是否成功
   */
  public boolean batchInsertWithAffair(String[] sqls) {
    boolean success = true;
    try {
      Class.forName(driver);
      cn = DriverManager.getConnection(url.toString());
      cn.setAutoCommit(false);
      for (int i = 0; i < sqls.length; i++) {
        ps = cn.prepareStatement(sqls[i]);
        ps.addBatch();
        System.out.println("第" + i + "条记录插入成功");
      }
      ps.executeBatch();
      System.out.println("批量操作无异常, 全部提交");
      cn.commit();//无异常再提交
    } catch (Exception e) {
      success = false;
      try {
        cn.rollback();
      } catch (SQLException e1) {
        e1.printStackTrace();
      }
      e.printStackTrace();
      System.out.println("增删改查失败");
    } finally {
      try {
        cn.setAutoCommit(true);
      } catch (SQLException e) {
        e.printStackTrace();
      }
      this.closeAll();
    }
    return success;
  }

  /**
   * 关闭数据库资源
   */
  private void closeAll() {
    //关闭资源 后打开先关闭
    try {
      if (rs != null) {
        rs.close();
      }
      if (ps != null) {
        ps.close();
      }
      if (cn != null) {
        cn.close();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("资源关闭异常");
    }
    System.out.println("正常-关闭资源");
  }
}