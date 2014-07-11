package org.notebook.gui.captcha;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.proxy.CaptchaStatus;
import org.http.channel.proxy.RemoteStatus;
import org.notebook.services.CaptchaListener;

public class CaptchaListModel extends AbstractTableModel implements  CaptchaListener{
	private static Log log = LogFactory.getLog("captcha");

	public Map<String, CaptchaItem> dataMap = new HashMap<String, CaptchaItem>();

	public LinkedList<CaptchaItem> data = null;
	protected DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	protected Timer t = new Timer();
	protected long lastNotifyTime = 0;
	
	public CaptchaListModel() {
		this.data = new LinkedList<CaptchaItem>();
		
	}

	String[] columns = new String[] {"ID", "验证码图片", "手工输入", "进入时间" };

	public String getColumnName(int column) {
		return columns[column];
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public int getColumnCount() {
		return this.columns.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex >= data.size())
			return "";
		CaptchaItem task = data.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return task.sid;
		case 1:
			return task.captcha; // .toString();
		case 2:
			return task.inputCode;
		case 3:
			return task.enterDate;
		}
		return "";
	}

	public boolean isCellEditable(int row, int col) {
		// Note that the data/cell address is constant,
		// no matter where the cell appears onscreen.

		return col == 2;
	}

	public void setValueAt(Object value, int row, int col) {
		// data[row][col] = value;
		if (col == 2) {
			data.get(row).inputCode = value + "";
		}
		
	}
	
	public int nextWaitingItem(int start){
		start = start < 0 ? 0 : start;
		for(; start < data.size(); start ++){
			if(!data.get(start).isDone){
				return start;
			}
		}
		
		return data.size() - 1;
	}



	@Override
	public void updateStatus(CaptchaStatus status) {
		CaptchaItem item = dataMap.get(status.sid);
		
		if(item != null && status.status != null){
			
			if(status.status.equals(CaptchaStatus.ST_REASSIGN) ||
					status.status.equals(CaptchaStatus.ST_TIMEOUT)
			 ){
				item.isDone = true;
			}
		}
		
	}
	
	public int getPaddingCount(){
		int  c = 0;
		try{
			long validTime = System.currentTimeMillis() - 1000 * 60 * 5;
			for(CaptchaItem i : data){
				if(!i.isDone && i.enterDate.getTime() > validTime){
					c++;
				}
			}
		}catch(Exception e){			
		}
		return c;
	}

	@Override
	public void syncRouteStatus(RemoteStatus status) {
	}

	@Override
	public synchronized void findCaptcha(org.http.channel.proxy.CaptchaItem item) {
		// TODO Auto-generated method stub
		
		//newItem.captcha = ImageIO.read(new ByteArrayInputStream(item.content));
		
		if(!dataMap.containsKey(item.sid)){
			CaptchaItem newItem = new CaptchaItem();
			newItem.sid = item.sid;
			newItem.captcha = new ImageIcon(item.content);
			newItem.inputCode = "";
			newItem.isDone = false;

			data.add(newItem);
			dataMap.put(newItem.sid, newItem);
			
			this.fireTableRowsInserted(data.size(), data.size());
			log.info("Get new captcah, sid:" + newItem.sid + ", immage size:" + 
					(item.content == null ? "null" : item.content.length));
			
			
			if(System.currentTimeMillis() - lastNotifyTime > 1 * 1000){
				new Thread(){
					public void run(){
						playNotice();
					}
				}.start();
			}
		}else {
			CaptchaItem t = dataMap.get(item.sid);
			if(t != null){
				t.isDone = false;
				t.inputCode = "";
			}		
			log.info("The captcah is already in list, sid:" + item.sid);			
		}
		
	}
	
	/**
	 * 清理过期的数据。
	 * @param curRow
	 */
	public int cleanExpiredRow(int curRow){
		int remainCount = 10;
		if(curRow > remainCount && getRowCount() > curRow + 10){
			for(int i = 0; i < curRow - remainCount; i++){
				CaptchaItem e = data.remove(0);
				dataMap.remove(e.sid);
				log.info("Remove captcha from input list, sid:" + e.sid);			
			}
			return remainCount;
		}
		return 0;		
	}
	
	public void playNotice(){
		lastNotifyTime = System.currentTimeMillis();
	   try{
		   URL u = getClass().getClassLoader().getResource("notice.wav");
		   if(u != null){
		        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(u);
		        Clip clip = AudioSystem.getClip();
		        clip.open(audioInputStream);
		        clip.start();
		        Thread.sleep(3000);
		        clip.close();
		        audioInputStream.close();
		   }else {
			   System.out.println("Not found wav.");
		   }
	    }catch(Exception ex){
	        System.out.println("Error with playing sound.");
	        ex.printStackTrace();
	    }
	}
	
}
