package com.github.newiarch;

import java.io.File;

public class ImageItem {
	private File image;
	private String title;
	private String lastModified;

	public ImageItem(File image, String title, String lastModified) {
		super();
		this.image = image;
		this.title = title;
		this.lastModified = lastModified;
	}

	public File getImage() {
		return image;
	}

	public void setImage(File image) {
		this.image = image;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}
	
}