package com.tea.server;

import com.tea.constants.XSMBSqlStatement;
import com.tea.db.jdbc.DbManager;
import com.tea.lib.ParseData;
import com.tea.util.NinjaUtils;
import com.tea.xsmb.XSMBService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class SummaryXMSB {
    private static final SummaryXMSB instance = new SummaryXMSB();

    public static SummaryXMSB getInstance() {
        return instance;
    }

    public void sum(int hour, int minute, int second) {
        NinjaUtils.schedule(() -> {
            summaryXSMB();
            summaryXSMB_prize();
        }, hour, minute, second);
    }

    private void summaryXSMB() {
        try {
            URL url = new URL(Config.getInstance().getXSMBUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Thiết lập phương thức GET
            connection.setRequestMethod("GET");

            // Thiết lập các header (nếu có)
            connection.setRequestProperty("Accept", "application/json");

            // Kiểm tra mã phản hồi từ server
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Nếu thành công, đọc phản hồi từ API
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                // Đóng các luồng
                in.close();
                connection.disconnect();
                JSONObject data = (JSONObject) JSONValue.parseWithException(content.toString());
                ParseData parseData = new ParseData(data);
                JSONObject t = (JSONObject) JSONValue.parse(parseData.getString("t"));
                ParseData parseT = new ParseData(t);
                JSONArray listIssueList = (JSONArray) JSONValue.parse(parseT.getString("issueList"));
                JSONObject issueList = (JSONObject) JSONValue.parse(listIssueList.get(0).toString());
                ParseData parseissueList = new ParseData(issueList);
                JSONArray details = (JSONArray) JSONValue.parse(parseissueList.getString("detail"));
                String ketqua = details.toString().replace("\"", "");
                ketqua = ketqua.replace("[", "");
                ketqua = ketqua.replace("]", "");
                PreparedStatement stmt = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(XSMBSqlStatement.INSERT_XSMB, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                stmt.setString(1, ketqua);
                try {
                    Integer result = stmt.executeUpdate();
                    if (result == 1) {
                        System.out.println("Tổng hợp kết quả xổ số thành công");
                    } else {
                        System.out.println("Tổng hợp kết quả xổ số có lỗi");
                    }
                }finally {
                    stmt.close();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void summaryXSMB_prize() {
        try {
            PreparedStatement statement = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(XSMBSqlStatement.GET_ALL_PLAYER_ORDER_XSMB);
            ResultSet resultSet = statement.executeQuery();
            try {
                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String ketQuaHomNay = XSMBService.getInstance().getKetQua(LocalDate.now()).get(0);
                    List<String> ketqua = Arrays.stream(ketQuaHomNay.split(",")).toList();
                    Long totalMoneyToday = XSMBService.getInstance().totalPrizeByDate(name, ketqua, LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/YYYY")));
                    if (totalMoneyToday > 0) {
                        PreparedStatement statementInsert = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(XSMBSqlStatement.INSERT_XSMB_SUM);
                        statementInsert.setString(1, name);
                        statementInsert.setLong(2, totalMoneyToday);
                        try {
                            Integer res = statementInsert.executeUpdate();
                            if (res == 1) {
                                System.out.println("summaryXSMB_prize name:" + name + " success!");
                            } else {
                                System.out.println("summaryXSMB_prize name:" + name + " failed!");
                            }
                        }
                        finally {
                            statementInsert.close();
                        }
                    }
                }
            }finally {
                statement.close();
                resultSet.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
