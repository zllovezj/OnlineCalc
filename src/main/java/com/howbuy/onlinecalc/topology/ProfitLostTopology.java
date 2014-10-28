package com.howbuy.onlinecalc.topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;

import com.howbuy.onlinecalc.bolt.CalcJoinBolt;
import com.howbuy.onlinecalc.bolt.CalcRsNotifyBolt;
import com.howbuy.onlinecalc.bolt.DimensionSplitBolt;
import com.howbuy.onlinecalc.bolt.HalfYearCalcBolt;
import com.howbuy.onlinecalc.bolt.MonthlyCalcBolt;
import com.howbuy.onlinecalc.bolt.TriMonthCalcBolt;
import com.howbuy.onlinecalc.spout.TriggerCalcSpout;

/**
 * ����ӯ�����������.
 * @author li.zhang
 *
 */
public class ProfitLostTopology
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        /**
         * storm�ķ������groupingֻ�Ǿ��������ĸ��ڵ���ĸ�������ִ�����bolt, ������������ͼ�е����boltִ�в�ִ��;��ʵ�ϣ�
         * 
         * ����ͼ�ϵ�ÿ��bolt���ǻᱻִ�е�.
         */
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("trigger-calc", new TriggerCalcSpout());
        builder.setBolt("dimension_split", new DimensionSplitBolt()).shuffleGrouping("trigger-calc");
        
        builder.setBolt("monthly_calc", new MonthlyCalcBolt()).allGrouping("dimension_split");    //һ���µĻ������ӯ������.
        builder.setBolt("tri_month_calc", new TriMonthCalcBolt()).allGrouping("dimension_split"); //�����µĻ������ӯ������.
        builder.setBolt("half_year_calc", new HalfYearCalcBolt()).allGrouping("dimension_split"); //����Ļ������ӯ������.
        
        builder.setBolt("calc_join", new CalcJoinBolt()).fieldsGrouping("monthly_calc", new Fields("fundcode")); //�������ȵ�ִ�н��join.
        builder.setBolt("calc_join", new CalcJoinBolt()).fieldsGrouping("tri_month_calc", new Fields("fundcode"));
        builder.setBolt("calc_join", new CalcJoinBolt()).fieldsGrouping("half_year_calc", new Fields("fundcode"));
        
        builder.setBolt("calc_result_notify", new CalcRsNotifyBolt()).shuffleGrouping("calc_join"); //������֪ͨ.
        
        Config conf = new Config();
        conf.setDebug(true);
        
        try
        {
            if (null == args || 0 >= args.length)
            {
                LocalCluster cluster = new LocalCluster();
                cluster.submitTopology("calc_fund_profit_lost", conf, builder.createTopology());
                Thread.sleep(1000);
                cluster.shutdown();
            }
            else
            {
                StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}