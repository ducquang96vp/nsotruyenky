package com.tea.event;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tea.constants.*;
import com.tea.db.jdbc.DbManager;
import com.tea.event.eventpoint.EventPoint;
import com.tea.event.eventpoint.Point;
import com.tea.item.Item;
import com.tea.item.ItemFactory;
import com.tea.item.ItemManager;
import com.tea.lib.RandomCollection;
import com.tea.map.zones.Zone;
import com.tea.model.Char;
import com.tea.option.ItemOption;
import com.tea.server.Config;
import com.tea.server.GlobalService;
import com.tea.util.Log;
import com.tea.util.NinjaUtils;
import lombok.Getter;
import lombok.Setter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class Event {

    public static final int NGAY_PHU_NU_VIET_NAM = 0;
    public static final int KOROKING = 1;
    public static final int TRUNG_THU = 2;
    public static final int HALLOWEEN = 3;
    public static final int NOEL = 4;
    public static final int LUNAR_NEW_YEAR = 5;
    public static final int WOMENS_DAY = 6;
    public static final byte SEA_GAME = 7;
    public static final int SUMMER = 8;

    public static final byte DOI_BANG_LUONG = 0;
    public static final byte DOI_BANG_XU = 1;

    public static final long EXPIRE_7_DAY = 604800000L;
    public static final long EXPIRE_5_DAY = 432000000L;
    public static final long EXPIRE_3_DAY = 259200000L;  // hạn 3 ngày
    public static final long EXPIRE_30_DAY = 2592000000L;

    public static final RandomCollection<Integer> DOI_PHAN_TU = new RandomCollection<>();

    static {
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_BUOM_BUOM_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_HOA_SEN_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_KEO_QUAN_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_MAT_TRANG_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_MAT_TROI_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_NGOI_SAO_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_TRAI_TIM_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_TRON_THOI_TRANG);
    }

    private static Event instance;

    public static void init() {
        if (Config.getInstance().getEvent() != null) {
            try {
                instance = (Event) Class.forName(Config.getInstance().getEvent()).newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                Log.error(ex.getMessage(), ex);
            }
        }
    }

    public static Event getEvent() {
        return instance;
    }

    public static boolean isEvent() {
        return (instance != null && !instance.isEnded());
    }

    public static boolean isTrungThu() {
        return isEvent() && instance instanceof TrungThu;//2  lỗi
    }

    public static boolean isSummer() {
        return isEvent() && instance instanceof SumMer;
    }

    public static boolean isKoroKing() {
        return isEvent() && instance instanceof KoroKing;//1  lỗi
    }

    public static boolean isVietnameseWomensDay() {
        return isEvent() && instance instanceof VietnameseWomensDay;//0 
    }

    public static boolean isInternationalWomensDay() {
        return isEvent() && instance instanceof InternationalWomensDay;//6  
    }

    public static boolean isHalloween() {
        return isEvent() && instance instanceof Halloween;//3 
    }

    public static boolean isNoel() {
        return isEvent() && instance instanceof Noel;//4 được  
    }

    public static boolean isLunarNewYear() {
        return isEvent() && instance instanceof LunarNewYear;//5 
    }

    public static TrungThu getTrungThu() {
        if (instance instanceof TrungThu) {
            return (TrungThu) instance;
        }
        return null;
    }

    public static LunarNewYear getLunarNewYear() {
        if (instance instanceof LunarNewYear) {
            return (LunarNewYear) instance;
        }
        return null;
    }

    public static Noel getNoel() {
        if (instance instanceof Noel) {
            return (Noel) instance;
        }
        return null;
    }

    public void doiLongDen(Char p, byte type, int index) {
        List<Item> list = p.getListItemByID(ItemName.LONG_DEN_TRON, ItemName.LONG_DEN_CA_CHEP, ItemName.LONG_DEN_MAT_TRANG, ItemName.LONG_DEN_NGOI_SAO);
        if (index < 0 || index >= list.size()) {
            return;
        }
        if (!isEvent()) {
            p.getService().npcChat(NpcName.TIEN_NU, "Sự kiện đã kết thúc!");
            return;
        }
        if (type == DOI_BANG_LUONG) {
            if (p.user.gold < 100) {
                p.getService().npcChat(NpcName.TIEN_NU, "Không đủ lượng!");
                return;
            }
            p.addLuong(-100);
        }
        if (type == DOI_BANG_XU) {
            if (p.coin < 2000000) {
                p.getService().npcChat(NpcName.TIEN_NU, "Không đủ xu!");
                return;
            }
            p.addXu(-2000000);
        }
        Item item = list.get(index);
        p.removeItem(item.index, 1, true);
        int itemID = DOI_PHAN_TU.next();
        Item itm = ItemFactory.getInstance().newItem(itemID);
        for (ItemOption o : item.options) {
            itm.options.add(o);
        }

        if (NinjaUtils.nextInt(200) == 0) {
            itm.options.add(new ItemOption(ItemOptionName.MIEN_GIAM_SAT_THUONG_POINT_PERCENT_TYPE_8, NinjaUtils.nextInt(1, 30)));
        }

        if (type == DOI_BANG_LUONG) {
            itm.randomOptionLongDen();
        }

        itm.expire = System.currentTimeMillis() + EXPIRE_30_DAY;
        p.themItemToBag(itm);
        p.getService().endDlg(true);
    }

    public static void useVipEventItem(Char _char, int type, RandomCollection<Integer> rc) {
        int itemId = rc.next();
        Item itm = ItemFactory.getInstance().newItem(itemId);
        itm.isLock = false;
        itm.expire = System.currentTimeMillis();

        long month = NinjaUtils.nextInt(1, type == 2 ? 3 : 2);
        long expire = month * ConstTime.MONTH;
        itm.expire += expire;

        if (type == 2 && NinjaUtils.nextInt(1, 35) == 1) {
            itm.expire = -1;
        }

        if (itm.id == ItemName.MAT_NA_HO) {
            itm.randomOptionTigerMask();
        }

        _char.themItemToBag(itm);
    }

    @Getter
    @Setter
    protected int id;
    protected List<EventPoint> eventPoints;
    @Getter
    protected RandomCollection<Integer> itemsThrownFromMonsters;
    @Getter
    protected RandomCollection<Integer> itemsRecFromCoinItem;
    @Getter
    protected RandomCollection<Integer> itemsRecFromGoldItem;
    @Getter
    protected RandomCollection<Integer> itemsRecFromGold2Item;
    protected Set<String> keyEventPoint;
    protected Calendar endTime = Calendar.getInstance();

    public Event() {
        itemsThrownFromMonsters = new RandomCollection<>();
        itemsRecFromCoinItem = new RandomCollection<>();
        itemsRecFromGoldItem = new RandomCollection<>();
        itemsRecFromGold2Item = new RandomCollection<>();
        eventPoints = new ArrayList<>();
        keyEventPoint = new TreeSet<>();
        initRandomItemHalowen();
    }

    public abstract void initStore();

    public int randomItemID() {
        return itemsThrownFromMonsters.next();
    }

    public abstract void action(Char p, int type, int amount);

    public abstract void menu(Char p);

    public void useItem(Char _char, Item item) {
    }

    public EventPoint createEventPoint() {
        EventPoint eventPoint = new EventPoint();
        keyEventPoint.forEach((key) -> {
            eventPoint.add(new Point(key, 0, 0));
        });
        return eventPoint;
    }

    public void loadEventPoint() {
        try {
            eventPoints.clear();
            PreparedStatement ps = DbManager.getInstance().getConnection(DbManager.GAME).prepareStatement(SQLStatement.LOAD_EVENT_POINT);
            ps.setInt(1, this.id);
            ps.setInt(2, Config.getInstance().getServerID());
            ResultSet rs = ps.executeQuery();
            Gson g = new Gson();
            while (rs.next()) {
                EventPoint eventPoint = createEventPoint();
                int id = rs.getInt("id");
                int playerID = rs.getInt("player_id");
                String name = rs.getString("name");
                ArrayList<Point> points = g.fromJson(rs.getString("point"), new TypeToken<ArrayList<Point>>() {
                }.getType());
                eventPoint.setId(id);
                eventPoint.setPlayerID(playerID);
                eventPoint.setPlayerName(name);
                eventPoint.setPoints(points);
                eventPoint.addIfMissing(keyEventPoint);
                eventPoints.add(eventPoint);
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            Logger.getLogger(Event.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addEventPoint(EventPoint eventPoint) {
        synchronized (eventPoints) {
            eventPoints.add(eventPoint);
        }
    }

    public void removeEventPoint(EventPoint eventPoint) {
        synchronized (eventPoints) {
            eventPoints.remove(eventPoint);
        }
    }

    public EventPoint findEventPointByPlayerID(int playerID) {
        synchronized (eventPoints) {
            for (EventPoint ev : eventPoints) {
                if (ev.getPlayerID() == playerID) {
                    return ev;
                }
            }
            return null;
        }
    }

    public abstract void initMap(Zone zone);

    // use only 1 item
    public boolean useEventItem(Char p, int itemId, RandomCollection<Integer> rc) {
        int[][] itemRequires = new int[][]{{itemId, 1}};
        return useEventItem(p, 1, itemRequires, 0, 0, 0, rc);
    }

    public boolean useEventItem(Char p, int number, int[][] itemRequire, int gold, int coin, int yen, RandomCollection<Integer> rc) {
        return makeEventItem(p, number, itemRequire, gold, coin, yen, rc, -1);
    }

    public boolean useEventItem(Char p, int number, int gold, int coin, RandomCollection<Integer> rc) {
        return makeEventItem(p, number, new int[][]{}, gold, coin, 0, rc, -1);
    }

    public boolean makeEventItem(Char p, int number, int[][] itemRequire, int gold, int coin, int yen, int itemIdReceive) {
        return makeEventItem(p, number, itemRequire, gold, coin, yen, null, itemIdReceive);
    }

    public boolean makeEventItem(Char p, int number, int[][] itemRequire, int gold, int coin, int yen, RandomCollection<Integer> rc, int itemIdReceive) {
        if (number < 1) {
            p.getService().npcChat(NpcName.TIEN_NU, "Số lượng tối thiểu là 1.");
            return false;
        }

        if (number > 1000) {
            p.getService().npcChat(NpcName.TIEN_NU, "Số lượng tối đa là 1.000.");
            return false;
        }

        int priceGold = number * gold;
        int priceCoin = number * coin;
        int priceYen = number * yen;

        for (int i = 0; i < itemRequire.length; i++) {
            int itemId = itemRequire[i][0];
            int amount = itemRequire[i][1] * number;
            int index = p.getIndexItemByIdInBag(itemId);
            if (index == -1 || p.bag[index] == null || !p.bag[index].has(amount)) {
                p.getService().npcChat(NpcName.TIEN_NU, "Không đủ " + ItemManager.getInstance().getItemName(itemId));
                return false;
            }
        }
        if (p.yen < priceYen) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ yên");
            return false;
        } else if (p.user.gold < priceGold) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ lượng");
            return false;
        } else if (p.coin < priceCoin) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ xu");
            return false;
        } else if (rc != null && p.getSlotNull() < number) {
            p.getService().npcChat(NpcName.TIEN_NU, p.language.getString("BAG_FULL"));
            return false;
        } else if (itemIdReceive != -1 && p.getSlotNull() < 1) {
            p.getService().npcChat(NpcName.TIEN_NU, p.language.getString("BAG_FULL"));
            return false;
        }

        if (priceYen > 0) {
            p.addYen(-priceGold);
        }
        if (priceGold > 0) {
            p.addLuong(-priceGold);
        }

        if (priceCoin > 0) {
            p.addXu(-priceCoin);
        }

        for (int i = 0; i < itemRequire.length; i++) {
            int itemId = itemRequire[i][0];
            int amount = itemRequire[i][1] * number;
            int index = p.getIndexItemByIdInBag(itemId);
            p.removeItem(index, amount, true);
        }

        if (rc != null) {
            for (int i = 0; i < number; i++) {
                int itemId = rc.next();
                Item itm = ItemFactory.getInstance().newItem(itemId);
                Item itmUsed = ItemFactory.getInstance().newItem(itemRequire[0][0]); // item used
                itm.initExpire();
                if (itm.id == ItemName.THONG_LINH_THAO) {
                    itm.setQuantity(NinjaUtils.nextInt(5, 10));
                } else if (itm.id == ItemName.MAT_NA_HO) {
                    itm.randomOptionTiger();
                }
                Random random = new Random();
                int expPercentage = 50; // 50% cho p.addExp
                int expPercentage1 = 1;
                int randomValue = random.nextInt(100);
                if (randomValue < expPercentage) {
                    p.addExp(8000000);
                } else if (randomValue == expPercentage1) {
                    p.addExp(20000000);
                    GlobalService.getInstance().chat("Hệ thống", "Người chơi " + Char.setNameVip(p.name) + " sử dụng " + itmUsed.template.name + " nhận được 20 triệu Kinh nghiệm");
                } else {
                    p.themItemToBag(itm);
                    // ktg
                    if (itemId == ItemName.BAT_BAO || itemId == ItemName.RUONG_BACH_NGAN || itemId == ItemName.RUONG_HUYEN_BI || itemId == ItemName.HARLEY_DAVIDSON) {
                        GlobalService.getInstance().chat("Hệ thống", "Người chơi " + Char.setNameVip(p.name) + " sử dụng " + itmUsed.template.name + " nhận được " + itm.template.name);
                    }
                }
            }

        } else if (itemIdReceive != -1) {
            Item itm = ItemFactory.getInstance().newItem(itemIdReceive);
            itm.setQuantity(number);
            p.themItemToBag(itm);
            p.getEventPoint().addPoint(EventPoint.DUNGHOPBANH, 1);
            if (priceGold > 0) {
                p.getEventPoint().addPoint(EventPoint.DUNGHOPBANH, 1);
            }
        }
        return true;
    }

    public void viewTop(Char p, String key, String title, String format) {
        List<EventPoint> list = eventPoints.stream().sorted((o1, o2) -> {
            int p1 = o1.getPoint(key);
            int p2 = o2.getPoint(key);
            return p2 - p1;
        }).limit(10).filter(t -> t.getPoint(key) > 0).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (EventPoint t : list) {
            sb.append(String.format(format, rank++, t.getPlayerName(), NinjaUtils.getCurrency(t.getPoint(key)))).append("\n");
        }
        p.getService().showAlert(title, sb.toString());
    }

    public int getRanking(Char p, String key) {
        List<EventPoint> list = eventPoints.stream().sorted((o1, o2) -> {
            int p1 = o1.getPoint(key);
            int p2 = o2.getPoint(key);
            return p2 - p1;
        }).limit(10).filter(t -> t.getPoint(key) > 0).collect(Collectors.toList());
        int rank = 0;
        for (EventPoint t : list) {
            rank++;
            if (t.getPlayerName().equals(p.name)) {
                return rank;
            }
        }
        return 99;
    }

    public boolean isEnded() {
        return endTime.getTime().getTime() - System.currentTimeMillis() <= 0;
    }

    public void initRandomItem() {
        itemsRecFromCoinItem.add(0.5, ItemName.RUONG_BACH_NGAN);
        itemsRecFromCoinItem.add(10, ItemName.BAT_BAO);
        itemsRecFromCoinItem.add(2, ItemName.LONG_KHI);
        itemsRecFromCoinItem.add(8, ItemName.HOA_TUYET);
        itemsRecFromCoinItem.add(15, ItemName.HUYEN_TINH_NGOC);
        itemsRecFromCoinItem.add(15, ItemName.HUYET_NGOC);
        itemsRecFromCoinItem.add(15, ItemName.LAM_TINH_NGOC);
        itemsRecFromCoinItem.add(15, ItemName.LUC_NGOC);
        itemsRecFromCoinItem.add(10, ItemName.XE_MAY);
        itemsRecFromCoinItem.add(35, ItemName.XICH_NHAN_NGAN_LANG);
        itemsRecFromCoinItem.add(15, ItemName.LONG_DEN_TRON);
        itemsRecFromCoinItem.add(15, ItemName.LONG_DEN_CA_CHEP);
        itemsRecFromCoinItem.add(15, ItemName.LONG_DEN_NGOI_SAO);
        itemsRecFromCoinItem.add(15, ItemName.LONG_DEN_MAT_TRANG);
        itemsRecFromCoinItem.add(60, ItemName.BANH_RANG);
        itemsRecFromCoinItem.add(80, ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_SO);
        itemsRecFromCoinItem.add(40, ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_TRUNG);
        itemsRecFromCoinItem.add(80, ItemName.DA_CAP_9);
        itemsRecFromCoinItem.add(3, ItemName.DA_CAP_10);
        itemsRecFromCoinItem.add(80, ItemName.MINH_MAN_DAN);
        itemsRecFromCoinItem.add(30, ItemName.LONG_LUC_DAN);
        itemsRecFromCoinItem.add(80, ItemName.KHANG_THE_DAN);
        itemsRecFromCoinItem.add(80, ItemName.SINH_MENH_DAN);
        itemsRecFromCoinItem.add(3, ItemName.BI_KIP_CUNG);
        itemsRecFromCoinItem.add(3, ItemName.BI_KIP_DAO);
        itemsRecFromCoinItem.add(3, ItemName.BI_KIP_KIEM_THUAT);
        itemsRecFromCoinItem.add(3, ItemName.BI_KIP_KUNAI);
        itemsRecFromCoinItem.add(3, ItemName.BI_KIP_QUAT);
        itemsRecFromCoinItem.add(3, ItemName.BI_KIP_TIEU_THUAT);
        itemsRecFromCoinItem.add(20, ItemName.HOAN_LUONG_CHI_THAO);
        itemsRecFromCoinItem.add(20, ItemName.GIAY_RACH);
        itemsRecFromCoinItem.add(20, ItemName.GIAY_BAC);
        itemsRecFromCoinItem.add(10, ItemName.THONG_LINH_THAO);
        itemsRecFromCoinItem.add(1, ItemName.BANH_TRUNG_THU_PHONG_LOI);
        itemsRecFromCoinItem.add(1, ItemName.BANH_TRUNG_THU_BANG_HOA);
        itemsRecFromCoinItem.add(2, ItemName.TU_TINH_THACH_SO_CAP);
        itemsRecFromCoinItem.add(1, ItemName.TU_TINH_THACH_TRUNG_CAP);


        // item receive from gold item
        itemsRecFromGoldItem.add(10, ItemName.HOAN_LUONG_CHI_THAO);
        itemsRecFromGoldItem.add(0.09, ItemName.RUONG_HUYEN_BI);
        itemsRecFromGoldItem.add(0.3, ItemName.RUONG_BACH_NGAN);
        itemsRecFromGoldItem.add(10, ItemName.BAT_BAO);
        itemsRecFromGoldItem.add(15, ItemName.HOA_TUYET);
        itemsRecFromGoldItem.add(45, ItemName.IK);
        itemsRecFromGoldItem.add(25, ItemName.HUYEN_TINH_NGOC);
        itemsRecFromGoldItem.add(25, ItemName.HUYET_NGOC);
        itemsRecFromGoldItem.add(25, ItemName.LAM_TINH_NGOC);
        itemsRecFromGoldItem.add(25, ItemName.LUC_NGOC);
        itemsRecFromGoldItem.add(25, ItemName.XE_MAY);
        itemsRecFromGoldItem.add(25, ItemName.MAT_NA_SUPER_BROLY);
        itemsRecFromGoldItem.add(25, ItemName.MAT_NA_ONNA_BUGEISHA);
        itemsRecFromGoldItem.add(40, ItemName.XICH_NHAN_NGAN_LANG);
        itemsRecFromGoldItem.add(0.1, ItemName.HUYET_SAC_HUNG_LANG);
        itemsRecFromGoldItem.add(1, ItemName.HARLEY_DAVIDSON);
        itemsRecFromGoldItem.add(50, ItemName.BANH_RANG);
        itemsRecFromGoldItem.add(15, ItemName.LONG_DEN_TRON);
        itemsRecFromGoldItem.add(15, ItemName.LONG_DEN_CA_CHEP);
        itemsRecFromGoldItem.add(30, ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_SO);
        itemsRecFromGoldItem.add(20, ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_TRUNG);
        itemsRecFromGoldItem.add(60, ItemName.DA_CAP_8);
        itemsRecFromGoldItem.add(20, ItemName.DA_CAP_9);
        itemsRecFromGoldItem.add(1, ItemName.DA_CAP_10);
        itemsRecFromGoldItem.add(30, ItemName.MINH_MAN_DAN);
        itemsRecFromGoldItem.add(30, ItemName.LONG_LUC_DAN);
        itemsRecFromGoldItem.add(30, ItemName.KHANG_THE_DAN);
        itemsRecFromGoldItem.add(30, ItemName.SINH_MENH_DAN);
        itemsRecFromGoldItem.add(5, ItemName.BI_KIP_CUNG);
        itemsRecFromGoldItem.add(5, ItemName.BI_KIP_DAO);
        itemsRecFromGoldItem.add(5, ItemName.BI_KIP_KIEM_THUAT);
        itemsRecFromGoldItem.add(2, ItemName.BI_KIP_KUNAI);
        itemsRecFromGoldItem.add(1, ItemName.BI_KIP_QUAT);
        itemsRecFromGoldItem.add(1, ItemName.BI_KIP_TIEU_THUAT);
        itemsRecFromGoldItem.add(10, ItemName.HAC_NGUU);
        itemsRecFromGoldItem.add(50, ItemName.THONG_LINH_THAO);
        itemsRecFromGoldItem.add(5, ItemName.HAKAIRO_YOROI);
        itemsRecFromGoldItem.add(20, ItemName.MANH_NON_JIRAI_);
        itemsRecFromGoldItem.add(20, ItemName.MANH_AO_JIRAI_);
        itemsRecFromGoldItem.add(20, ItemName.MANH_QUAN_JIRAI_);
        itemsRecFromGoldItem.add(20, ItemName.MANH_GANG_TAY_JIRAI_);
        itemsRecFromGoldItem.add(20, ItemName.MANH_GIAY_JIRAI_);
        itemsRecFromGoldItem.add(20, ItemName.MANH_PHU_JIRAI_);
        itemsRecFromGoldItem.add(20, ItemName.GIAY_RACH);
        itemsRecFromGoldItem.add(20, ItemName.GIAY_BAC);
        itemsRecFromGoldItem.add(20, ItemName.GIAY_VANG);
        itemsRecFromGoldItem.add(5, ItemName.HAGGIS);
        itemsRecFromGoldItem.add(10, ItemName.PHA_LE);
        itemsRecFromGoldItem.add(10, ItemName.LONG_DEN);
        itemsRecFromGoldItem.add(10, ItemName.NHAM_THACH_);
        itemsRecFromGoldItem.add(5, ItemName.MAT_NA_INU);
        itemsRecFromGoldItem.add(5, ItemName.PET_BONG_MA);
        itemsRecFromGoldItem.add(5, ItemName.PET_YEU_TINH);
        itemsRecFromGoldItem.add(5, ItemName.MAT_NA_HO);
        itemsRecFromGoldItem.add(0.1, ItemName.CHIM_TINH_ANH);
        itemsRecFromGoldItem.add(5, ItemName.HOA_KY_LAN);
        itemsRecFromGoldItem.add(20, ItemName.DA_DANH_VONG_CAP_1);
        itemsRecFromGoldItem.add(15, ItemName.DA_DANH_VONG_CAP_2);
        itemsRecFromGoldItem.add(20, ItemName.VIEN_LINH_HON_CAP_1);
        itemsRecFromGoldItem.add(15, ItemName.VIEN_LINH_HON_CAP_2);
        itemsRecFromGoldItem.add(1, ItemName.BANH_TRUNG_THU_BANG_HOA);
        itemsRecFromGoldItem.add(1, ItemName.BANH_TRUNG_THU_PHONG_LOI);
        itemsRecFromGoldItem.add(1, ItemName.NHAN_SAM_NGAN_NAM);

        // item receive from gold 2 item
        itemsRecFromGold2Item.add(0.5, ItemName.RUONG_BACH_NGAN);
        itemsRecFromGold2Item.add(10, ItemName.BAT_BAO);
        itemsRecFromGold2Item.add(10, ItemName.DA_CAP_10);
        itemsRecFromGold2Item.add(10, ItemName.DA_CAP_11);
        itemsRecFromGold2Item.add(10, ItemName.LONG_DEN_TRON);
        itemsRecFromGold2Item.add(10, ItemName.LONG_DEN_CA_CHEP);
        itemsRecFromGold2Item.add(10, ItemName.LONG_LUC_DAN);
        itemsRecFromGold2Item.add(30, ItemName.SINH_MENH_DAN);
        itemsRecFromGold2Item.add(6, ItemName.GIAY_BAC);
        itemsRecFromGold2Item.add(3, ItemName.GIAY_VANG);
        itemsRecFromGold2Item.add(2, ItemName.XICH_NHAN_NGAN_LANG);
        itemsRecFromGold2Item.add(3, ItemName.HAGGIS);
        itemsRecFromGold2Item.add(5, ItemName.HAC_NGUU);
        itemsRecFromGold2Item.add(5, ItemName.LAN_SU_VU);
        itemsRecFromGold2Item.add(1, ItemName.BANH_TRUNG_THU_BANG_HOA);
        itemsRecFromGold2Item.add(1, ItemName.BANH_TRUNG_THU_PHONG_LOI);
        itemsRecFromGold2Item.add(1, ItemName.MANH_SACH_CO);
        itemsRecFromGold2Item.add(5, ItemName.VIEN_LINH_HON_CAP_2);
        itemsRecFromGold2Item.add(5, ItemName.VIEN_LINH_HON_CAP_3);
        itemsRecFromGold2Item.add(5, ItemName.VIEN_LINH_HON_CAP_4);
        itemsRecFromGold2Item.add(5, ItemName.VIEN_LINH_HON_CAP_5);
        itemsRecFromGold2Item.add(5, ItemName.VIEN_LINH_HON_CAP_6);
        itemsRecFromGold2Item.add(5, ItemName.VIEN_LINH_HON_CAP_7);
        itemsRecFromGold2Item.add(5, ItemName.VIEN_LINH_HON_CAP_8);
        itemsRecFromGold2Item.add(5, ItemName.VIEN_LINH_HON_CAP_9);
        itemsRecFromGold2Item.add(0.5, ItemName.PET_YEU_TINH);
        itemsRecFromGold2Item.add(0.5, ItemName.MAT_NA_HO);
        itemsRecFromGold2Item.add(0.05, ItemName.MAT_NA_ONI);
        itemsRecFromGold2Item.add(1, ItemName.QUA_CHAKA_XANH);
        itemsRecFromGold2Item.add(1, ItemName.QUA_CHAKA_VANG);
        itemsRecFromGold2Item.add(0.005, ItemName.TRUNG_VI_THU);
        itemsRecFromGold2Item.add(1, 1294);
        itemsRecFromGold2Item.add(5, ItemName.HOA_TUYET);
        itemsRecFromGold2Item.add(3, ItemName.TU_TINH_THACH_SO_CAP);
        itemsRecFromGold2Item.add(1, ItemName.KHI_BAO);
        itemsRecFromGold2Item.add(1, ItemName.LANG_BAO);

        itemsRecFromGold2Item.add(0.1, ItemName.PHAN_THAN_LENH);
        itemsRecFromGold2Item.add(10, ItemName.TOM_HUM);
        itemsRecFromGold2Item.add(5, ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_SO);
        itemsRecFromGold2Item.add(5, ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_TRUNG);
        itemsRecFromGold2Item.add(5, ItemName.DIA_LANG_THAO);
        itemsRecFromGold2Item.add(5, ItemName.TAM_LUC_DIEP);
        itemsRecFromGold2Item.add(2, ItemName.CHIM_TINH_ANH);
        itemsRecFromGold2Item.add(0.5, ItemName.BAO_HIEM_CAO_CAP);
        itemsRecFromGold2Item.add(0.5, ItemName.XE_MAY);
        itemsRecFromGold2Item.add(0.001, ItemName.HARLEY_DAVIDSON);
    }

    public void initRandomItemHalowen() {
        this.itemsRecFromCoinItem.add(0.5, 384);
        this.itemsRecFromCoinItem.add(10.0, 383);
        this.itemsRecFromCoinItem.add(2.0, 821);
        this.itemsRecFromCoinItem.add(8.0, 775);
        this.itemsRecFromCoinItem.add(15.0, 652);
        this.itemsRecFromCoinItem.add(15.0, 653);
        this.itemsRecFromCoinItem.add(15.0, 654);
        this.itemsRecFromCoinItem.add(15.0, 655);
        this.itemsRecFromCoinItem.add(10.0, 485);
        this.itemsRecFromCoinItem.add(35.0, 443);
        this.itemsRecFromCoinItem.add(15.0, 568);
        this.itemsRecFromCoinItem.add(15.0, 569);
        this.itemsRecFromCoinItem.add(15.0, 570);
        this.itemsRecFromCoinItem.add(15.0, 571);
        this.itemsRecFromCoinItem.add(60.0, 576);
        this.itemsRecFromCoinItem.add(80.0, 436);
        this.itemsRecFromCoinItem.add(40.0, 437);
        this.itemsRecFromCoinItem.add(80.0, 8);
        this.itemsRecFromCoinItem.add(3.0, 9);
        this.itemsRecFromCoinItem.add(80.0, 275);
        this.itemsRecFromCoinItem.add(30.0, 276);
        this.itemsRecFromCoinItem.add(80.0, 277);
        this.itemsRecFromCoinItem.add(80.0, 278);
        this.itemsRecFromCoinItem.add(3.0, 400);
        this.itemsRecFromCoinItem.add(3.0, 401);
        this.itemsRecFromCoinItem.add(3.0, 397);
        this.itemsRecFromCoinItem.add(3.0, 399);
        this.itemsRecFromCoinItem.add(3.0, 402);
        this.itemsRecFromCoinItem.add(3.0, 398);
        this.itemsRecFromCoinItem.add(20.0, 257);
        this.itemsRecFromCoinItem.add(20.0, 549);
        this.itemsRecFromCoinItem.add(20.0, 550);
        this.itemsRecFromGoldItem.add(10.0, 257);
        this.itemsRecFromGoldItem.add(0.1, 385);
        this.itemsRecFromGoldItem.add(0.6, 384);
        this.itemsRecFromGoldItem.add(10.0, 383);
        this.itemsRecFromGoldItem.add(15.0, 775);
        this.itemsRecFromGoldItem.add(45.0, 577);
        this.itemsRecFromGoldItem.add(25.0, 652);
        this.itemsRecFromGoldItem.add(25.0, 653);
        this.itemsRecFromGoldItem.add(25.0, 654);
        this.itemsRecFromGoldItem.add(25.0, 655);
        this.itemsRecFromGoldItem.add(25.0, 485);
        this.itemsRecFromGoldItem.add(25.0, 407);
        this.itemsRecFromGoldItem.add(25.0, 408);
        this.itemsRecFromGoldItem.add(40.0, 443);
        this.itemsRecFromGoldItem.add(0.1, 523);
        this.itemsRecFromGoldItem.add(1.0, 524);
        this.itemsRecFromGoldItem.add(50.0, 576);
        this.itemsRecFromGoldItem.add(15.0, 568);
        this.itemsRecFromGoldItem.add(15.0, 569);
        this.itemsRecFromGoldItem.add(30.0, 436);
        this.itemsRecFromGoldItem.add(20.0, 437);
        this.itemsRecFromGoldItem.add(60.0, 7);
        this.itemsRecFromGoldItem.add(20.0, 8);
        this.itemsRecFromGoldItem.add(1.0, 9);
        this.itemsRecFromGoldItem.add(30.0, 275);
        this.itemsRecFromGoldItem.add(30.0, 276);
        this.itemsRecFromGoldItem.add(30.0, 277);
    }
}
