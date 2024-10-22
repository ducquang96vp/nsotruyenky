package com.tea.xsmb;

import com.tea.constants.XSMBSqlStatement;
import com.tea.db.jdbc.DbManager;
import com.tea.lib.ParseData;
import com.tea.model.Char;
import com.tea.server.Config;
import com.tea.server.GlobalService;
import com.tea.util.NinjaUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class SummaryXMSBService {
    private static final SummaryXMSBService instance = new SummaryXMSBService();
    private static final DecimalFormat formatterMoney = new DecimalFormat("###,###");

    public static SummaryXMSBService getInstance() {
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
                Integer result = stmt.executeUpdate();
                if (result == 1) {
                    System.out.println("Summary XSMB Success");
                } else {
                    System.out.println("Summary XSMB Failed");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void summaryXSMB_prize() {
        try {
            PreparedStatement statement = DbManager.getInstance().getConnection().prepareStatement(XSMBSqlStatement.GET_ALL_PLAYER_ORDER_XSMB);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String ketQuaHomNay = XSMBService.getInstance().getKetQua(LocalDate.now()).get(0);
                List<String> ketqua = Arrays.stream(ketQuaHomNay.split(",")).toList();
                Long totalMoneyToday = XSMBService.getInstance().totalPrizeByDate(name, ketqua, LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/YYYY")));
                if (totalMoneyToday > 0) {
                    PreparedStatement statementInsert = DbManager.getInstance().getConnection().prepareStatement(XSMBSqlStatement.INSERT_XSMB_SUM);
                    statementInsert.setString(1, name);
                    statementInsert.setLong(2, totalMoneyToday);
                    Integer res = statementInsert.executeUpdate();
                    if (res == 1) {
                        System.out.println("summaryXSMB_prize name:" + name + " success!");
                        GlobalService.getInstance().chat("Xổ số", "Người chơi " +name + " đã trúng sổ xố " + formatterMoney.format(totalMoneyToday) + " xu ");
                    } else {
                        System.out.println("summaryXSMB_prize name:" + name + " failed!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
