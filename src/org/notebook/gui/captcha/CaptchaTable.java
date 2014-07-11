package org.notebook.gui.captcha;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import org.notebook.gui.MenuToolbar;
import org.notebook.services.BookController;

public class CaptchaTable extends JTable {
	protected static final String DONE_EDIT = "doneInput";
	protected static final String MOVE_NEXT = "moveNext";
	protected static final String MOVE_UP = "moveUp";
	public BookController controller = null;
	
	protected CaptchaListModel m = null;
	
	public CaptchaTable(){
		super(new CaptchaListModel());
		
		m = (CaptchaListModel)getModel();
		initUI();
		regiesterKeyAction();
		
		m.addTableModelListener(new TableModelListener(){
			public void tableChanged(TableModelEvent e) {
				
				if(e.getType() == TableModelEvent.INSERT){
					final int start = e.getFirstRow();					
					int editingRow = getEditingRow();
					
					if(editingRow < 0){
						editingRow = start - 1;
					}else{
						CaptchaItem item = m.data.get(editingRow);
						if(item != null && !item.isDone){
							System.out.println("not change editing row");							
						}else {
							editingRow = start - 1;
						}
					}
					
					moveToRow(editingRow);
				}
			}
		});
	}
	
	public void initUI(){
		setRowSelectionAllowed(false);  
		setColumnSelectionAllowed(false);  
		setCellSelectionEnabled(false);  
		
		getColumnModel().getColumn(1).setCellRenderer(new ImageRenderer());
		getColumnModel().getColumn(2).setCellRenderer(new CaptchaRenderer());
		getColumnModel().getColumn(2).setCellEditor(new PanelCellEditorRenderer());
		getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());

		getColumnModel().getColumn(0).setMaxWidth(80);
		getColumnModel().getColumn(1).setPreferredWidth(200);
		
		setRowMargin(20);
		setRowHeight(100);
	}
	
	protected void regiesterKeyAction(){
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
				DONE_EDIT);
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
				MOVE_NEXT);
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
				MOVE_UP);
		
		getActionMap().put(DONE_EDIT, new DoneEditAction());
		getActionMap().put(MOVE_NEXT, new MoveNextAction());
		getActionMap().put(MOVE_UP, new MoveUpAction());
		
		//this.getModel().g
	}
	
	public void changeSelection(final int row, final int column, boolean toggle, boolean extend){
         super.changeSelection(row, column, toggle, extend);
         if(column == 2){
         	this.editCellAt(row, column);
         }
         this.transferFocus();
     }
	
	
	protected void moveToRow(int moveTo){
		if(moveTo < 0) {
			moveTo = 0;
		}else if(moveTo >= m.data.size()){
			moveTo = m.data.size() - 1;
		}
		
		final int toRow = moveTo;
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
				System.out.println("changed, editingRow:" + (toRow));
				if(toRow >= 0 && toRow < m.data.size()){
					scrollRectToVisible(new Rectangle(getCellRect(toRow, 2, true)));
			        
					editCellAt(toRow, 2);
			        transferFocus();
				}
		    }
		  });
	}
	
	class PanelCellEditorRenderer extends AbstractCellEditor implements TableCellEditor {

	    private static final long serialVersionUID = 1L;
	    private CompCellPanel renderer = new CompCellPanel();
	    private CompCellPanel editor = new CompCellPanel();

	    @Override
	    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
	        editor.f.setText(value + "");
	        return editor;
	    }

		@Override
		public Object getCellEditorValue() {
			return editor.f.getText();
		}


	}
	
	class ImageRenderer extends DefaultTableCellRenderer {

		  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
		      boolean hasFocus, int row, int column) {
			  JLabel lbl = new JLabel();

			  if(value != null && value instanceof ImageIcon){
				  lbl.setIcon((Icon)value);
			  }else {
				  lbl.setText(value + "");
			  }
		    return lbl;
		  }
	}

	class StatusRenderer extends DefaultTableCellRenderer {

		  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
		      boolean hasFocus, int row, int column) {
			  JLabel lbl = new JLabel();			  
			  CaptchaItem item = m.data.get(row);

			  Font font = new Font(Font.SANS_SERIF, Font.BOLD, 24);
			  lbl.setFont(font);
			  lbl.setForeground(Color.black);

			  if(item.isDone){
				  lbl.setText("DONE");
			  }else {
				  long st = System.currentTimeMillis() - item.enterDate.getTime();
				  st = st / 1000;
				  
				  lbl.setForeground(Color.red);
				  lbl.setText(st + "s");				  
			  }
			  
			  return lbl;
		  }
	}	
	
	class CaptchaRenderer extends DefaultTableCellRenderer {

		  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
		      boolean hasFocus, int row, int column) {
			  JLabel lbl = new JLabel();

			  lbl.setText(value.toString());
			  Font font = new Font(Font.SANS_SERIF, Font.BOLD, 24);
			  lbl.setFont(font);
			  lbl.setForeground(Color.BLUE);
			  
		    return lbl;
		  }
	}
	
	
	class CompCellPanel extends JPanel {
		public JTextField f = null;
		public CompCellPanel(){
			super(new BorderLayout());
			f = new JTextField();
			
			f.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
					MOVE_NEXT);
			f.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
					MOVE_UP);
			
			
			//getActionMap().put(DONE_EDIT, new DoneEditAction());
			f.getActionMap().put(MOVE_NEXT, new MoveNextAction());
			f.getActionMap().put(MOVE_UP, new MoveUpAction());

			Font font = new Font(Font.SANS_SERIF, Font.BOLD, 24);
			f.setFont(font);
			f.setForeground(Color.BLUE);
			//	this.add(comp)
			
			add(f, BorderLayout.CENTER);
		}
		
		
		
		
	}
	
	class DoneEditAction extends AbstractAction {
	    @Override
	    public void actionPerformed(ActionEvent e) {
	    	
	    	int row = getEditingRow();
	        if(getCellEditor()!=null){
	            getCellEditor().stopCellEditing();
	        }
	        
	        if(row < 0) return;
	        
	        CaptchaItem item = m.data.get(row);
	        if(item.inputCode != null && item.inputCode.trim().length() > 0){
		        item.isDone = true;
		        
		        controller.dispatchEvent(MenuToolbar.CAPTCHA_INPUT_DONE, item);
		       
		        row = m.nextWaitingItem(row);
		        moveToRow(row);
	        }else{
	        	 if(row < m.data.size() - 1){
	        		 moveToRow(row + 1);
	        	 }else {
	        		 moveToRow(row);
	        	 }
	        }
	    }
	}

	class MoveUpAction extends AbstractAction {
	    @Override
	    public void actionPerformed(ActionEvent e) {
	    	int row = getEditingRow();
	        if(getCellEditor()!=null){
	            getCellEditor().stopCellEditing();
	        }
	        
	        moveToRow(row - 1);
	    }
	}
	
	class MoveNextAction extends AbstractAction {
	    @Override
	    public void actionPerformed(ActionEvent e) {
	        int row = getEditingRow();

	        if(getCellEditor()!=null){
	            getCellEditor().stopCellEditing();
	        }
	        
	        moveToRow(row + 1);
	    }
	}

}
