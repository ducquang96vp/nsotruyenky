package com.tea.xsmb;

import com.tea.constants.CMDInputDialog;
import com.tea.constants.CMDMenu;
import com.tea.constants.XSMBSqlStatement;
import com.tea.db.jdbc.DbManager;
import com.tea.model.Char;
import com.tea.model.InputDialog;
import com.tea.model.Menu;
import com.tea.server.Config;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class XSMBService {
    private static final XSMBService instance = new XSMBService();
    private static final Integer multiple = Config.getInstance().getMultiple();
    private static final Integer multipleMoney = Config.getInstance().getMultipleMoney();
    private static final DecimalFormat decimalFormat = new DecimalFormat("###,###");

    public static XSMBService getInstance() {
        return instance;
    }

    public void order(Char player, String qty) {
        Integer lotoQty = 0;
        String numberStr;
        Integer number = 0;
        try {
            try {
                if (qty.equals("")) {
                    player.getService().serverDialog("Số điểm không hợp lệ.");
                    return;
                }
                lotoQty = Integer.parseInt(qty.split("d")[0]);
                if (lotoQty < 10 || lotoQty > 10000) {
                    player.getService().serverDialog("Số điểm phải trong khoảng từ 10 đến 10,000.");
                    return;
                }
                numberStr = qty.split("d")[1];
                number = Integer.parseInt(qty.split("d")[1]);
                if (numberStr.length() != 2 && (number < 00 || number > 99)) {
                    player.getService().serverDialog("Số đặt không hợp lệ.");
                    return;
                }
            } catch (Exception e) {
                player.getService().serverDialog("Cú pháp không hợp lệ.");
                return;
            }
            PreparedStatement stmt = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(XSMBSqlStatement.GET_TOTAL_ORDER_XSMB_ORDER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setString(1, player.name);
            stmt.setInt(2, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            res.next();
            try {
                Integer maxQty;
                if (res.getRow() > 0) {
                    maxQty = res.getInt("qty") + lotoQty;
                    if (maxQty >= 10000) {
                        player.getService().serverDialog("Một ngày chỉ được đặt tối đa 10,000 điểm.");
                        return;
                    }
                }
                maxQty = lotoQty;
                Long value = maxQty.longValue() * multiple;
                if (player.getCoin() < value) {
                    player.getService().serverDialog("Xu con không đủ, nạp thêm đi con");
                    return;
                }
                PreparedStatement stmtInsertExists = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(XSMBSqlStatement.CHECK_EXISTS_NUMBER_XSMB, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                stmtInsertExists.setString(1, player.name);
                stmtInsertExists.setLong(2, number);
                ResultSet result = stmtInsertExists.executeQuery();
                result.next();
                int count = result.getInt("count");
                if (count > 0) {
                    PreparedStatement stmtUpdateSMB = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(XSMBSqlStatement.UPDATE_XSMB_ORDER);
                    stmtUpdateSMB.setInt(1, maxQty);
                    stmtUpdateSMB.setLong(2, value);
                    stmtUpdateSMB.setString(3, player.name);
                    stmtUpdateSMB.setInt(4, number);
                    stmtUpdateSMB.setLong(5, Config.getInstance().getServerID());
                    Integer xsmbUpdate = stmtUpdateSMB.executeUpdate();
                    if (xsmbUpdate > 0) {
                        String text = "Đặt " + lotoQty + " điểm lô " + numberStr + " thành công";
                        player.getService().serverDialog(text);
                        player.addXu(-value);
                        player.getService().serverMessage(text + " -" + decimalFormat.format(value) + " xu");
                    } else {
                        player.getService().serverDialog("Đã có lỗi sảy ra. Vui lòng liên hệ admin để được hỗ trợ");
                    }
                } else {
                    PreparedStatement stmtInsertXSMB = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(XSMBSqlStatement.INSERT_XSMB_ORDER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    stmtInsertXSMB.setString(1, player.name);
                    stmtInsertXSMB.setLong(2, maxQty);
                    stmtInsertXSMB.setInt(3, Config.getInstance().getServerID());
                    stmtInsertXSMB.setLong(4, value);
                    stmtInsertXSMB.setInt(5, number);
                    Integer xsmbUpdate = stmtInsertXSMB.executeUpdate();
                    if (xsmbUpdate == 1) {
                        String text = "Đặt " + lotoQty + " điểm lô " + numberStr + " thành công";
                        player.addXu(-value);
                        player.getService().serverMessage(text + " -" + decimalFormat.format(value) + " xu");
                    } else {
                        player.getService().serverDialog("Đã có lỗi sảy ra. Vui lòng liên hệ admin để được hỗ trợ");
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                player.getService().serverDialog("Đã có lỗi sảy ra. Vui lòng liên hệ admin để được hỗ trợ");
            } finally {
                res.close();
                stmt.close();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void showHisOrder(Char player) {
        try {
            ResultSet res = getOrderHis(player.name, LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/YYYY")));
            StringBuilder strData = new StringBuilder();
            StringBuilder strHist = new StringBuilder();
            strData.append("Lịch sử đặt lệnh của con đây:\n");
            try {
                while (res.next()) {
                    strHist.append("- Lô: " + res.getString("number") + ": " + res.getString("qty") + " điểm.\n");
                }
                if (strHist.toString().isEmpty()) {
                    strHist.append("Con đã chơi đâu mà vào xem lịch sử");
                }
                player.getService().showAlert("Lịch sử", strData.toString() + strHist + totalPrizeSum(player.name));

            } catch (Exception ex) {
                ex.printStackTrace();
                player.getService().serverDialog("Có lỗi sảy ra. Vui lòng liên hệ admin để được hỗ trợ");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<String> getKetQua(LocalDate Date) {
        try {
            PreparedStatement statement = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(XSMBSqlStatement.GET_XSMB, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.setString(1, Date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            String ketqua = resultSet.getString("ket_qua");
            String tradingDate = resultSet.getString("trading_date");
            List<String> response = List.of(ketqua, tradingDate);
            return response;
        } catch (Exception e) {
            return new ArrayList<>();
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
            List<String> ketqua = getKetQua(LocalDate.now());
            if (ketqua.isEmpty()) {
                ketqua = getKetQua(LocalDate.now().minusDays(1));
            }
            String[] array = ketqua.get(0).split(",");
            giaiDacBiet = "G.ĐB|               " + array[0];
            giaiNhat = "G.1|                  " + array[1];
            giaiNhi = "G.2|          " + array[2] + " - " + array[2];
            giaiBa = "G.3|      " + array[4] + " - " + array[5] + " - " + array[6] + "\n            " + array[7] + " - " + array[8] + " - " + array[9];
            giaiTu = "G.4|    " + array[10] + " - " + array[11] + " - " + array[12] + " - " + array[13];
            giaiNam = "G.5|       " + array[14] + " - " + array[15] + " - " + array[16] + "\n             " + array[17] + " - " + array[18] + " - " + array[19];
            giaiSau = "G.6|          " + array[20] + " - " + array[21] + " - " + array[22];
            giaiBay = "G.7|         " + array[23] + " - " + array[24] + " - " + array[25] + " - " + array[26];

            StringBuilder strKetQua = new StringBuilder();
            strKetQua.append("Ngày " + ketqua.get(1) + "\n\n");
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
                }
            }
            return totalPrizeMoney;
        } catch (Exception ex) {
            return 0L;
        }
    }

    public ResultSet getOrderHis(String name, String date) {
        try {
            PreparedStatement stmt = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(XSMBSqlStatement.GET_XSMB_ORDER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setString(1, name);
            stmt.setInt(2, Config.getInstance().getServerID());
            stmt.setString(3, date);
            ResultSet res = stmt.executeQuery();
            if (res != null) return res;
            else return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void huongDan(Char player) {
        StringBuilder builder = new StringBuilder();
        builder.append("1. Đặt lô từ 0h-18h tối hằng ngày.\n");
        builder.append("2. Cú pháp:\n [Số điểm] + [d] + [Số lô]\n");
        builder.append("3. Ví dụ: 100 điểm lô 10 \n=> Cú pháp: 100d10\n");
        builder.append("4. Mức giá: 25,000 xu/điểm\n");
        builder.append("5. Tỉ lệ: 1 điểm ăn 80,000 xu\n");
        builder.append("6. Tối thiểu 10 điểm/lần\n");
        builder.append("7. Tối đa 10,000 điểm/ngày\n");
        builder.append("8. Tổng hợp thưởng vào 18h40p\n");
        builder.append("9. Nhận thưởng sau 18h40p\n");
        player.getService().showAlert("Hướng dẫn", builder.toString());
    }

    public void nhanThuong(Char player) {
        Long totalPrizeMoney = 0L;
        try {
            PreparedStatement statement = DbManager.getInstance().getConnection(DbManager.XSMB).prepareStatement(XSMBSqlStatement.GET_XSMB_PRIZE);
            statement.setString(1, player.name);
            ResultSet res = statement.executeQuery();
            while (res.next()) {
                totalPrizeMoney += res.getLong("total");
            }
            if (totalPrizeMoney == 0L) {
                player.getService().serverMessage("Con hãy đặt lệnh và chờ kết quả con nhé");
                return;
            }
            player.addXu(totalPrizeMoney);
            DbManager.getInstance().update(XSMBSqlStatement.UPDATE_XSMB_SUM, player.name);
            String message = "Nhận thưởng xổ số " + decimalFormat.format(totalPrizeMoney) +" thành công";
            player.getService().serverMessage(message);
            player.getService().chatGlobal("Xổ số", "Người chơi: " + player.name.toUpperCase() + " đã trúng xổ số " + decimalFormat.format(totalPrizeMoney));
        } catch (Exception ex) {
            player.getService().serverDialog("Có lỗi sảy ra. Vui lòng liên hệ admin để được hỗ trợ");
            ex.printStackTrace();
        }
    }

    public String totalPrizeSum(String name) {
        HashMap<String, Integer> mapCount = new HashMap<>();
        StringBuilder summary = new StringBuilder();
        summary.append("\nTổng hợp trúng thưởng:\n");
        LocalDate dateLocal = LocalDate.now().minusDays(6);
        for (int i = 0; i < 7; i++) {
            List<String> listKetQua = getKetQua(dateLocal);
            if (listKetQua.isEmpty()) {
                dateLocal = dateLocal.plusDays(1);
                continue;
            }
            Long totalPrizeMoney = 0L;
            try {
                Arrays.stream(listKetQua.get(0).split(",")).toList().stream().forEach(item -> {
                    String subItem = item.substring(item.length() - 2);
                    if (!mapCount.containsKey(subItem)) {
                        mapCount.put(subItem, 1);
                    } else {
                        mapCount.put(subItem, mapCount.get(subItem) + 1);
                    }
                });
                ResultSet res = getOrderHis(name, dateLocal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                summary.append("- Ngày: " + dateLocal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ":\n");
                while (res.next()) {
                    if (mapCount.containsKey(res.getString("number"))) {
                        Long prizeMoney = 0L;
                        prizeMoney += res.getLong("qty") * multipleMoney * mapCount.get(res.getString("number"));
                        totalPrizeMoney += prizeMoney;
                        summary.append(res.getString("number") + ": " + mapCount.get(res.getString("number")) + " x " + res.getLong("qty") + " = " + decimalFormat.format(prizeMoney) + "\n");
                    }
                }
                summary.append("=> Tổng : " + decimalFormat.format(totalPrizeMoney) + "\n");
                mapCount.clear();
                dateLocal = dateLocal.plusDays(1);
            } catch (Exception ex) {
                return summary.toString();
            }
        }
        return summary.toString();
    }

    public void addMenuXSMB(Char player) {
        player.menus.add(new Menu(CMDMenu.EXECUTE, "XSMB", () -> {
            if (!player.isHuman) {
                player.warningClone();
                return;
            }
            player.menus.clear();
            player.menus.add(new Menu(CMDMenu.EXECUTE, "Đặt", () -> {
                LocalTime now = LocalTime.now();
                LocalTime sixPM = LocalTime.of(18, 00);
                if (!now.isBefore(sixPM)) {
                    player.getService().serverDialog("Quá giờ rồi. Con hãy quay lại vào ngày hôm sau nhé");
                    return;
                }
                InputDialog input = new InputDialog(CMDInputDialog.LOTO, "Đặt Lô");
                player.setInput(input);
                player.getService().showInputDialog();
            }));
            player.menus.add(new Menu(CMDMenu.EXECUTE, "Tra cứu", () -> {
                showHisOrder(player);
            }));
            player.menus.add(new Menu(CMDMenu.EXECUTE, "Kết quả", () -> {
                getResult(player);
            }));
            player.menus.add(new Menu(CMDMenu.EXECUTE, "Nhận thưởng", () -> {
                nhanThuong(player);
            }));
            player.menus.add(new Menu(CMDMenu.EXECUTE, "Hướng dẫn", () -> {
                huongDan(player);
            }));
            player.getService().openUIMenu();
        })); // QuangDD xsmb
    }

}
