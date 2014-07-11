package org.notebook.gui.captcha;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.util.LinkedList;

import javax.swing.AbstractCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import org.notebook.gui.MenuToolbar;

public class CaptchaInputPanel extends JPanel{
	public CaptchaTable mainTable = null;
	
	public CaptchaInputPanel(){
		super(new BorderLayout());
				
		JTabbedPane textControlsPane = new JTabbedPane();
		
		mainTable = new CaptchaTable();		
				
		JScrollPane jsp = new JScrollPane(mainTable);		
		textControlsPane.addTab("正在录入", jsp);
		
		CaptchaListModel paddingList = new CaptchaListModel();
		JTable doneTable = new StatusTable(paddingList);
		jsp = new JScrollPane(doneTable);		
		textControlsPane.addTab("录入完成", jsp);
		

        add(textControlsPane, BorderLayout.CENTER);
        //p.add(buttons, BorderLayout.SOUTH);
	}
	
	
	class StatusTable extends JTable{
		public StatusTable(CaptchaListModel model){
			super(model);
			this.setGUI();
		}
		
		private void setGUI(){
			this.getColumnModel().getColumn(0).setPreferredWidth(200);
		}
	}
	



}
