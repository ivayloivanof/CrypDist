package GUI;

import Blockchain.Transaction;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 * Created by gizem on 06.04.2017.
 */
public class QueryScreen extends JPanel {

    JTextField query;
    //JTextArea results;
    GlossyButton run;
    GlossyButton back;
    ScreenManager controller;
    JTable results;
    DefaultTableModel resultsModel;
    ArrayList<Transaction> currTransactions;

    public QueryScreen(ScreenManager controller) {
        this.controller = controller;
        setSize((new Dimension(1000,600)));
        setBackground(Color.white);

        query = new JTextField();
        resultsModel = new DefaultTableModel() {
            public boolean isCellEditable(int row, int col)
            {
                return false;
            }
        };
        resultsModel.addColumn("");
        results = new JTable(resultsModel);
        results.addMouseListener(new TableListener());
        currTransactions = new ArrayList<>();
        currTransactions.add(null);

//        // TODO remove
        Object[] rowData = {"GeneX"};
        resultsModel.addRow(rowData);
        resultsModel.fireTableDataChanged();

        run = new GlossyButton("Run");
        back = new GlossyButton("Back");

        ButtonListener l = new ButtonListener();
        query.addActionListener(l);
        run.addActionListener(l);
        back.addActionListener(l);

        JScrollPane queryScroll = new JScrollPane(query);
        queryScroll.setBackground(Color.white);
        JScrollPane resultsScroll = new JScrollPane(results);
        resultsScroll.setBackground(Color.white);

        Border border = BorderFactory.createLineBorder(Color.GRAY, 3);

        queryScroll.setBorder(BorderFactory.createCompoundBorder(border,
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        resultsScroll.setBorder(BorderFactory.createCompoundBorder(border,
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        run.setBorder(BorderFactory.createCompoundBorder(border,
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        back.setBorder(BorderFactory.createCompoundBorder(border,
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.ipady = getHeight()/2 - 125;
        gbc.ipadx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 0.5;
        gbc.gridy = 0;
        gbc.gridwidth = 5;
        gbc.insets = new Insets(10,10,5,10);
        add(queryScroll,gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipady = 0;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5,10,5,10);
        add(run,gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipadx = 0;
        gbc.ipady = getHeight()/2 - 125;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(10,10,5,10);
        gbc.gridwidth = 5;
        add(resultsScroll,gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipady = 0;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.insets = new Insets(5,10,5,10);
        gbc.gridwidth = 1;
        add(back,gbc);

        repaint();
        setVisible(true);
    }

    class ButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            if(e.getSource() == run || e.getSource() == query)
            {
                back.setEnabled(false);
                // TODO: Check if query statement is correct
                if(query.getText().equals("")) {
                    JOptionPane.showMessageDialog(QueryScreen.this, "Please enter a query statement!", "Warning",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                resultsModel.setRowCount(0);
                currTransactions.clear();

                HashMap<String, ArrayList<Transaction>> queryResults = controller.query(query.getText());
                Set<String> keySet = queryResults.keySet();
                Iterator<String> iterator = keySet.iterator();
                while (iterator.hasNext())
                {
                    String key = iterator.next();
                    ArrayList<Transaction> transactions = queryResults.get(key);

                    for (int i = 0; i < transactions.size(); i++)
                    {
                        Object[] rowData = {transactions.get(i).getDataSummary()};
                        resultsModel.addRow(rowData);
                        resultsModel.fireTableDataChanged();
                        currTransactions.add(transactions.get(i));
                    }
                }
                back.setEnabled(true);
            }
            else {  // back
                controller.setCurrentView(new MainScreen(controller));
                controller.setSize((new Dimension(1000,600)));
            }
            repaint();
        }
    }

    class TableListener extends MouseAdapter
    {
        public void mouseClicked(MouseEvent event)
        {
            if (SwingUtilities.isRightMouseButton(event))
            {
                int row = results.rowAtPoint(event.getPoint());
                if (row >= 0 && row < currTransactions.size())
                {
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem download = new JMenuItem("Download");
                    JMenuItem update = new JMenuItem("Update");
                    popupMenu.add(download);
                    popupMenu.add(update);
                    popupMenu.setLocation(event.getXOnScreen(), event.getYOnScreen());
                    popupMenu.setVisible(true);
                    download.addActionListener(e -> {
                        update.setSelected(false);
                        String filename = currTransactions.get(row).getFileName();
                        controller.showDownload(filename);
                    });
                    update.addActionListener(e -> {
                        download.setSelected(false);
                        JFileChooser fileChooser = new JFileChooser();
                        String path = fileChooser.getSelectedFile().getAbsolutePath();
                        String fileName = fileChooser.getSelectedFile().getName();
                        try {
                            controller.updateData(currTransactions.get(row), path, fileName);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    });
                }
            }
        }
    }
}