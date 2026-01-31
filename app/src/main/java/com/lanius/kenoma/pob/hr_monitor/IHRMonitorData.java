package com.lanius.kenoma.pob.hr_monitor;

import java.util.EventListener;

public interface IHRMonitorData extends EventListener
	{
	    void onData(HxMData data);
	}

