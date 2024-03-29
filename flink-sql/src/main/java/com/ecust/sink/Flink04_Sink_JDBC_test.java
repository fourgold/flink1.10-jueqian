package com.ecust.sink;

import com.ecust.beans.SensorReading;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;

/**
 * @author JueQian
 * @create 01-22 11:51
 */
public class Flink04_Sink_JDBC_test {

    public static void main(String[] args) throws Exception {

        //1.获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        //获取TableAPI执行环境
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        //2.读取端口数据转换为JavaBean
        SingleOutputStreamOperator<SensorReading> sensorDS = env.socketTextStream("hadoop102", 9999)
                .map(line -> {
                    String[] fields = line.split(",");
                    return new SensorReading(fields[0],
                            Long.parseLong(fields[1]),
                            Double.parseDouble(fields[2]));
                });

        //3.SQL
        tableEnv.createTemporaryView("sensor", sensorDS);
        Table sqlResult = tableEnv.sqlQuery("select id from sensor");

        //4.定义MySQL DDL
        String sinkDDL = "create table jdbcOutputTable (" +
                " id varchar(20) not null" +
                ") with (" +
                " 'connector.type' = 'jdbc', " +
                " 'connector.url' = 'jdbc:mysql://hadoop102:3306/test', " +
                " 'connector.table' = 'sensor_bug', " +
                " 'connector.driver' = 'com.mysql.jdbc.Driver', " +
                " 'connector.username' = 'root', " +
                " 'connector.password' = '123456', " +
                " 'connector.write.flush.max-rows' = '1' )";
        tableEnv.sqlUpdate(sinkDDL);

        //5.将数据写入MySQL
        sqlResult.insertInto("jdbcOutputTable");

        //6.执行任务
        env.execute();

    }
}
