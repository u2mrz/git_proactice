package test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.After;
import org.junit.Test;

public class ValiasDbTest {

    @Test
    public void sqlite() throws Exception {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:test.sqlite3");
        Statement st = con.createStatement();
        executeUpdate(st, "drop table if exists person");
        executeUpdate(st, "create table person (id integer primary key, name string)");
        executeQuery();
    }

    @Test
    public void h2() throws Exception {
        con = DriverManager.getConnection("jdbc:h2:testh2", "sa", "");
        Statement st = con.createStatement();
        executeUpdate(st, "drop table if exists person");
        executeUpdate(st, "create table person (id integer primary key, name varchar)");
        executeQuery();
    }

    @Test
    public void derby() throws Exception {
        con = DriverManager.getConnection("jdbc:derby:derby;create=true");
        Statement st = con.createStatement();
        executeUpdate(st, "drop table person");
        executeUpdate(st, "create table person (id int primary key, name varchar(200))");
        executeQuery();
    }

    @Test
    public void mysql_myisam() throws Exception {
        mysql("MyISAM");
    }
    @Test
    public void mysql_innodb() throws Exception {
        mysql("InnoDB");
    }
    private void mysql(String engine) throws Exception {
        con = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "");
        Statement st = con.createStatement();
        executeUpdate(st, "drop table if exists person");
        executeUpdate(st, "create table person (id integer primary key, name varchar(200)) engine = " + engine);
        executeQuery(con.getMetaData().getDatabaseProductName() + "(" + engine + ")");
    }

    @Test
    public void postgres() throws Exception {
        con = DriverManager.getConnection("jdbc:postgresql:postgres", "postgres", "postgres");
        Statement st = con.createStatement();
        executeUpdate(st, "drop table if exists person");
        executeUpdate(st, "create table person (id integer primary key, name varchar)");
        executeQuery();
    }

    @Test
    public void cassandra() throws Exception {
        con = DriverManager.getConnection("jdbc:cassandra://localhost:9160/test");
        Statement st = con.createStatement();
        executeUpdate(st, "drop table person");
        executeUpdate(st, "create table person (id int primary key, name text)");
        executeQuery();
    }

    // 共通メンバー -------------------------------------------

    private Connection con;
    private static final int insertTime = 1000 * 5;
    private static final int COUNT = 10000 * 10;
    private static final String DATA = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";

    @After
    public void after() throws Exception {
        if (con != null) {
            con.close();
        }
    }

    private void executeUpdate(Statement st, String sql) {
        try {
            st.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private void executeQuery() throws Exception {
        executeQuery(con.getMetaData().getDatabaseProductName());
    }

    private void executeQuery(String databaseName) throws Exception {

        boolean isCassandra = databaseName.contains("Cassandra");
        boolean isAutoCommit = isCassandra;

        System.out.printf("%-14s", databaseName);
        if (!isAutoCommit) {
            con.setAutoCommit(false);
        }

        long insertStart = System.currentTimeMillis();
        PreparedStatement insertPs = con.prepareStatement("insert into person (id, name) values(?, '" + DATA + "')");
        int i = 0;
        while ((System.currentTimeMillis() - insertStart) >= insertTime) {
            insertPs.setInt(1, i);
            insertPs.executeUpdate();
            i++;
        }
        if (!isAutoCommit) {
            con.commit();
        }
        double insertSec = (double) (System.currentTimeMillis() - insertStart) / 1000;

        long selectStart = System.currentTimeMillis();
        PreparedStatement selectPs = con.prepareStatement("select * from person where id = ?");
        int insertCount = i;
        for (i = 0; i < insertCount; i++) {
            selectPs.setInt(1, i);
            selectPs.executeQuery().next();
        }
        double selectSec = (double) (System.currentTimeMillis() - selectStart) / 1000;

        String countSql = "select count(1) from person";
        if (isCassandra) {
            countSql += " limit 100000000";
        }
        ResultSet rs = con.createStatement().executeQuery(countSql);
        rs.next();
        logProcessTime(rs.getInt(1), insertSec, selectSec);
    }

    private void logProcessTime(long count, double insertSec, double selectSec) {

        System.out.printf("%4d万件 ", count / 10000);
        System.out.printf("%7.2f秒 ", insertSec);
        System.out.printf("%7.2f秒", selectSec);
        System.out.println();
    }
}
