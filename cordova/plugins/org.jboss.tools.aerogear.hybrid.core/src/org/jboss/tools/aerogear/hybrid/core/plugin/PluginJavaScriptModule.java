package org.jboss.tools.aerogear.hybrid.core.plugin;

import java.util.ArrayList;
import java.util.List;

public class PluginJavaScriptModule {
	
	private String name;
	private String source;
	private boolean runs;
	private List<String> merges;
	private List<String> clobbers;
	private String platform;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public boolean isRuns() {
		return runs;
	}
	public void setRuns(boolean runs) {
		this.runs = runs;
	}
	public List<String> getMerges() {
		return merges;
	}
	public void addMerge(String merge) {
		if(this.merges == null ){
			this.merges = new ArrayList<String>();
		}
		this.merges.add(merge);
	}
	public List<String> getClobbers() {
		return clobbers;
	}
	public void addClobber(String clobber) {
		if(this.clobbers == null){
			this.clobbers = new ArrayList<String>();
		}
		this.clobbers.add(clobber);
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}

}
