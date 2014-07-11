package org.notebook.gui.captcha;

import java.io.Serializable;
import java.util.Date;

import javax.swing.ImageIcon;

public class CaptchaItem implements Serializable {
	
	public String sid = "";
	public ImageIcon captcha = null;//MenuToolbar.icon("org/notebook/gui/images/application.png").getImage();
	
	public String inputCode = "code";
	public Date enterDate = new Date();

	public Date inputDate = new Date();
	public boolean isDone = false;
	
	
}
