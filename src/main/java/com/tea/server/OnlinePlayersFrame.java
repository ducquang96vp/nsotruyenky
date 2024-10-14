
package com.tea.server;

import com.tea.model.Char;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.TableView;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class OnlinePlayersFrame extends JFrame {
    private JTable table;
    private JScrollPane scrollPane;

    public OnlinePlayersFrame() {
        setTitle("Danh Sách Online");
        setSize(250, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponentsDetails();
        setVisible(true);
    }

    private void initComponents() {
        Vector<String> headers = new Vector<>();
        headers.add("Tên Nhân Vật");
        Vector<Vector<String>> data = getOnlineCharacters();
        DefaultTableModel model = new DefaultTableModel(data, headers);
        table = new JTable(model);

        scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }
    private void initComponentsDetails() {
        JTable table = getOnlineCharactersTable();

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(SwingConstants.LEFT); // Căn trái

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER); // Căn giữa

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT); // Căn phải
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);  // Cột STT căn giữa
        table.getColumnModel().getColumn(0).setPreferredWidth(20);  // Cột STT căn giữa
        table.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);  // Cột Tên nhân vật căn trái
        table.getColumnModel().getColumn(1).setPreferredWidth(100);  // Cột Tên nhân vật căn trái
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);  // Cột Level căn giữa
        table.getColumnModel().getColumn(2).setPreferredWidth(20);  // Cột Level căn giữa
        scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }
    private Vector<Vector<String>> getOnlineCharacters() {
        Vector<Vector<String>> data = new Vector<>();
        List<Char> characters = ServerManager.getChars();
        for (Char character : characters) {
            Vector<String> row = new Vector<>();
            row.add(character.setNameVip(character.name));
            data.add(row);
        }
        return data;
    }

    private JTable getOnlineCharactersTable() {
        String[] columnNames = {"STT", "Name", "Level"};
        DefaultTableModel model = new DefaultTableModel(null, columnNames);
        List<Char> characters = ServerManager.getChars();
        int count=1;
        for (Char character : characters) {
            model.addRow(new Object[] {count, character.name,character.level});
            count++;
        }
        JTable table = new JTable(model);

        return table;
    }

    public static void display() {
        new OnlinePlayersFrame();
    }
}
