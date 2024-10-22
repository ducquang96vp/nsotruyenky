package com.tea.constants;

public class XSMBSqlStatement {
    public static final String GET_XSMB_ORDER = "SELECT id, name, qty, number FROM `XSMB_ORDER` WHERE `name` = ? AND (`server` = ? OR `server` = 0) AND DATE(trading_date)=STR_TO_DATE(?,'%d/%m/%Y') order by trading_date desc Limit 7;";
    public static final String GET_TOTAL_ORDER_XSMB_ORDER = "SELECT sum(qty) qty FROM `XSMB_ORDER` WHERE `name` = ? AND (`server` = ? OR `server` = 0) AND DATE(trading_date)=DATE(now()) LIMIT 1;";
    public static final String UPDATE_XSMB_ORDER = "UPDATE  `XSMB_ORDER` SET `qty`=`qty`+?,`value`=`value`+? WHERE `name` = ? AND `number`=? AND (`server` = ? OR `server` = 0) AND DATE(`trading_date`) = DATE(now()) order by trading_date desc Limit 7;";
    public static final String INSERT_XSMB_ORDER = "INSERT INTO  `XSMB_ORDER`(`name`, `qty`, `server`, `trading_date`,`value`,`number`) VALUES(?,?,?,DATE(NOW()),?,?);";
    public static final String INSERT_XSMB = "INSERT INTO  `XSMB`(`ket_qua`, `trading_date`) VALUES(?,now());";
    public static final String GET_XSMB = "SELECT ket_qua,DATE_FORMAT(trading_date,'%d/%m/%Y') trading_date from xsmb where Date(trading_date)=str_to_date(?,'%d/%m/%Y') order by trading_date desc Limit 7";
    public static final String INSERT_XSMB_SUM = "INSERT INTO  `XSMB_SUM`(`name`,`total`, `trading_date`,`status`) VALUES(?,?,now(),0);";
    public static final String UPDATE_XSMB_SUM = "UPDATE `XSMB_SUM` set status=1 where name=?";
    public static final String CHECK_EXISTS_NUMBER_XSMB = "select count(1) as count from `XSMB_ORDER` where name=? and number=? and Date(trading_date)=Date(now()) ";
    public static final String GET_XSMB_PRIZE = "select `name`,`total`, `trading_date` from `XSMB_SUM` where `name`=? and status=0";
    public static final String GET_ALL_PLAYER_ORDER_XSMB = "select distinct(name) name from xsmb_order where Date(trading_date)=Date(now())";
}
