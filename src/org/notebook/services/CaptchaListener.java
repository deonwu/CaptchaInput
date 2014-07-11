package org.notebook.services;

import org.http.channel.proxy.CaptchaStatus;
import org.http.channel.proxy.CaptchaItem;
import org.http.channel.proxy.RemoteStatus;

public interface CaptchaListener {
	
	public void findCaptcha(CaptchaItem item);
	public void updateStatus(CaptchaStatus status);
	public void syncRouteStatus(RemoteStatus status);

}
