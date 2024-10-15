package com.tea.model;

import com.tea.constants.SQLStatement;
import com.tea.db.jdbc.DbManager;
import com.tea.server.Config;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

public class XSMB {
    private static final XSMB instance = new XSMB();
    private static final Integer multiple = Config.getInstance().getMultiple();
    private static final Integer multipleMoney = Config.getInstance().getMultipleMoney();
    private static final DecimalFormat decimalFormat = new DecimalFormat("###,###");

    public static XSMB getInstance() {
        return instance;
    }

    public void order(Char player, String qty) {
        Long lotoQty = 0L;
        String numberStr;
        Integer number = 0;
        try {
            try {
                if (qty.equals("")) {
                    player.getService().serverDialog("Số điểm không hợp lệ.");
                    return;
                }
                lotoQty = Long.parseLong(qty.split("d")[0]);
                if (lotoQty < 10 || lotoQty > 1000) {
                    player.getService().serverDialog("Số điểm phải trong khoảng từ 10 đến 1000.");
                    return;
                }
                numberStr = qty.split("d")[1];
                number = Integer.parseInt(qty.split("d")[1]);
                if (numberStr.length() < 2 && (number < 00 || lotoQty > 99)) {
                    player.getService().serverDialog("Số đặt không hợp lệ.");
                    return;
                }
            } catch (Exception e) {
                player.getService().serverDialog("Cú pháp không hợp lệ.");
                return;
            }
            PreparedStatement stmt = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(SQLStatement.GET_TOTAL_ORDER_XSMB_ORDER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setString(1, player.name);
            stmt.setInt(2, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            try {
                Long maxQty = 0L;
                if (res.getFetchSize() > 0) {
                    maxQty = Long.parseLong(res.getString(2)) + lotoQty;
                    if (maxQty >= 1000) {
                        player.getService().serverDialog("Một ngày chỉ được đặt tối đa 1000 điểm.");
                        return;
                    }
                    PreparedStatement stmtUpdateXSMB = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(SQLStatement.UPDATE_XSMB_ORDER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    stmtUpdateXSMB.setLong(1, maxQty);
                    stmtUpdateXSMB.setString(2, player.name);
                    stmtUpdateXSMB.setInt(3, Config.getInstance().getServerID());
                    stmtUpdateXSMB.setInt(4, number);
                    Integer result = stmt.executeUpdate();
                    if (result == 1) {
                        player.getService().serverDialog("Đặt " + lotoQty + " điểm lô thành công");
                    } else {
                        player.getService().serverDialog("Đã có lỗi sảy ra. Vui lòng liên hệ admin để được hỗ trợ");
                    }
                } else {
                    maxQty = lotoQty;
                    Long value = maxQty * multiple;
                    if (player.getCoin() < value) {
                        player.getService().serverDialog("Xu con không đủ, nạp thêm đi con");
                        return;
                    }
                    PreparedStatement stmtInsertXSMB = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(SQLStatement.INSERT_XSMB_ORDER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    stmtInsertXSMB.setString(1, player.name);
                    stmtInsertXSMB.setLong(2, maxQty);
                    stmtInsertXSMB.setInt(3, Config.getInstance().getServerID());
                    stmtInsertXSMB.setLong(4, value);
                    stmtInsertXSMB.setInt(5, number);
                    Integer result = stmtInsertXSMB.executeUpdate();
                    if (result == 1) {
                        String text = "Đặt " + lotoQty + " điểm lô " + numberStr + " thành công";
                        player.addXu(-value);
                        player.getService().serverMessage(text + " -" + value + " xu");
                    } else {
                        player.getService().serverDialog("Đã có lỗi sảy ra. Vui lòng liên hệ admin để được hỗ trợ");
                    }
                }
            } catch (Exception ex) {
                player.getService().serverDialog("Đã có lỗi sảy ra. Vui lòng liên hệ admin để được hỗ trợ");
            } finally {
                res.close();
                stmt.close();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void show(Char player) {
        try {
            ResultSet res = getOrderHis(player.name, LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/YYYY")));
            if (res == null) {
                player.getService().serverDialog("Con đã chơi đâu mà vào xem lịch sử");
                return;
            }
            StringBuilder strData = new StringBuilder();
            strData.append("Lịch sử đặt lệnh của con đây:\n");
            try {
                while (res.next()) {
                    strData.append("- Lô: " + res.getString("number") + ": " + res.getString("qty") + " điểm.\n");
                }
                player.getService().showAlert("Lịch sử", strData.toString());
            } catch (Exception ex) {
                ex.printStackTrace();
                player.getService().serverDialog("Có lỗi sảy ra. Vui lòng liên hệ admin để được hỗ trợ");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String ketquaToday() {
        try {
            PreparedStatement statement = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(SQLStatement.GET_XSMB, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            String ketqua = resultSet.getString("ket_qua");
            return ketqua;
        } catch (Exception e) {
            System.out.println(e.toString());
            return "";
        }
    }

    public void getResult(Char player) {
        String giaiDacBiet;
        String giaiNhat;
        String giaiNhi;
        String giaiBa;
        String giaiTu;
        String giaiNam;
        String giaiSau;
        String giaiBay;
        try {
            String ketqua = ketquaToday();
            String[] array = ketqua.split(",");
            giaiDacBiet = "G.ĐB|               " + array[0];
            giaiNhat = "G.1|                  " + array[1];
            giaiNhi = "G.2|          " + array[2] + " - " + array[2];
            giaiBa = "G.3|      " + array[4] + " - " + array[5] + " - " + array[6] + "\n            " + array[7] + " - " + array[8] + " - " + array[9];
            giaiTu = "G.4|    " + array[10] + " - " + array[11] + " - " + array[12] + " - " + array[13];
            giaiNam = "G.5|       " + array[14] + " - " + array[15] + " - " + array[16] + "\n             " + array[17] + " - " + array[18] + " - " + array[19];
            giaiSau = "G.6|          " + array[20] + " - " + array[21] + " - " + array[22];
            giaiBay = "G.7|         " + array[23] + " - " + array[24] + " - " + array[25] + " - " + array[26];

            StringBuilder strKetQua = new StringBuilder();
            strKetQua.append("Ngày " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n\n");
            strKetQua.append(giaiDacBiet + "\n");
            strKetQua.append(giaiNhat + "\n");
            strKetQua.append(giaiNhi + "\n");
            strKetQua.append(giaiBa + "\n");
            strKetQua.append(giaiTu + "\n");
            strKetQua.append(giaiNam + "\n");
            strKetQua.append(giaiSau + "\n");
            strKetQua.append(giaiBay + "\n");
            player.getService().showAlert("Kết quả xổ số", strKetQua.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Long totalPrizeByDate(String name, List<String> ketQua, String date) {
        HashMap<String, Integer> mapCount = new HashMap<>();
        ketQua.stream().forEach(item -> {
            String subItem = item.substring(item.length() - 2);
            if (!mapCount.containsKey(subItem)) {
                mapCount.put(subItem, 1);
            } else {
                mapCount.put(subItem, mapCount.get(subItem) + 1);
            }
        });
        ResultSet res = getOrderHis(name, date);
//        StringBuilder summary = new StringBuilder();
//        summary.append("Tổng hợp trúng thưởng:\n");
        Long totalPrizeMoney = 0L;

        try {
            if (res == null) {
                return totalPrizeMoney;
            }
            while (res.next()) {
                if (mapCount.containsKey(res.getString("number"))) {
                    Long prizeMoney = 0L;
                    prizeMoney += res.getLong("qty") * multipleMoney * mapCount.get(res.getString("number"));
                    totalPrizeMoney += prizeMoney;
//                    summary.append(res.getString("number") + ": " + mapCount.get(res.getString("number")) + " nháy x " + res.getLong("qty") + " điểm = " + decimalFormat.format(prizeMoney) + "\n");
                }
            }
//            summary.append("Tổng thưởng: " + decimalFormat.format(totalPrizeMoney));
            return totalPrizeMoney;
        } catch (Exception ex) {
            return 0L;
        }
    }

    public ResultSet getOrderHis(String name, String date) {
        try {
            PreparedStatement stmt = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(SQLStatement.GET_XSMB_ORDER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setString(1, name);
            stmt.setInt(2, Config.getInstance().getServerID());
            stmt.setString(3, date);
            ResultSet res = stmt.executeQuery();
            if(res!=null)
            return res;
            else return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void huongdan(Char player) {
        StringBuilder builder = new StringBuilder();
        builder.append("Đặt lô từ 0h sáng đến trước 18h tối hằng ngày.\n");
        builder.append("Mức giá: 25,000 xu/điểm\n");
        builder.append("Tối thiểu 100 điểm/lần\n");
        builder.append("Tối đa 1000 điểm/ngày\n");
        builder.append("Tổng hợp kết quả vào 19h tối hằng ngày\n");
        builder.append("Nhận thưởng sau 19h\n");
        builder.append("Mọi thắc mắc liên hệ admin để được xử lý.");
        player.getService().showAlert("Hướng dẫn", builder.toString());
    }

    public void nhanthuong(Char player) {
        Long totalPrizeMoney = 0L;
        try {
            PreparedStatement statement = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(SQLStatement.GET_XSMB_PRIZE);
            statement.setString(1,player.name);
            ResultSet res = statement.executeQuery();
            while (res.next()) {
                totalPrizeMoney += res.getLong("total");
            }
            player.addXu(totalPrizeMoney);
            PreparedStatement statementUpdate = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(SQLStatement.UPDATE_XSMB_SUM);
            statementUpdate.setString(1,player.name);
            Integer result= statement.executeUpdate();
            String message = "Nhận thưởng thành công: " + decimalFormat.format(totalPrizeMoney);
            player.getService().serverMessage(message);
            player.getService().chatGlobal("Xổ số", message);
        } catch (Exception ex) {
            player.getService().serverDialog("Có lỗi sảy ra. Vui lòng liên hệ admin để được hỗ trợ");
            ex.printStackTrace();
        }
    }
}
