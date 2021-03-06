package net.sf.mardao.domain;

public class MergeTemplate {
	private String templatePrefix = "";
	private String templateMiddle = "";
	private String templateSuffix = ".vm";
	private String destFolder = "targetDao";
	private String filePrefix = "";
	private String fileMiddle = "";
	private String fileSuffix = ".java";
	private boolean entity = true;
	private boolean typeSpecific = false;
	private boolean typeAppend = true;
	private boolean listingEntities = false;

        /**
         * @since 2.2.4
         */
        private String requiresOnClasspath = null;
	
	public void setListingEntities(boolean listingEntities) {
		this.listingEntities = listingEntities;
	}
	public String getTemplatePrefix() {
		return templatePrefix;
	}
	public void setTemplatePrefix(String templatePrefix) {
		this.templatePrefix = templatePrefix;
	}
	public String getTemplateSuffix() {
		return templateSuffix;
	}
	public void setTemplateSuffix(String templateSuffix) {
		this.templateSuffix = templateSuffix;
	}
	public String getDestFolder() {
		return destFolder;
	}
	public void setDestFolder(String destFolder) {
		this.destFolder = destFolder;
	}
	public String getFilePrefix() {
		return filePrefix;
	}
	public void setFilePrefix(String filePrefix) {
		this.filePrefix = filePrefix;
	}
	public String getFileSuffix() {
		return fileSuffix;
	}
	public void setFileSuffix(String fileSuffix) {
		this.fileSuffix = fileSuffix;
	}
	public boolean isEntity() {
		return entity;
	}
	public void setEntity(boolean entity) {
		this.entity = entity;
	}
	public boolean isTypeSpecific() {
		return typeSpecific;
	}
	public void setTypeSpecific(boolean typeSpecific) {
		this.typeSpecific = typeSpecific;
	}
	public String getTemplateMiddle() {
		return templateMiddle ;
	}
	public void setTemplateMiddle(String templateMiddle) {
		this.templateMiddle = templateMiddle;
	}
	public void setFileMiddle(String fileMiddle) {
		this.fileMiddle = fileMiddle;
	}
	public String getFileMiddle() {
		return fileMiddle;
	}
	public boolean isListingEntities() {
		return listingEntities;
	}
	public void setTypeAppend(boolean typeAppend) {
		this.typeAppend = typeAppend;
	}
	public boolean isTypeAppend() {
		return typeAppend;
	}

    public String getRequiresOnClasspath() {
        return requiresOnClasspath;
    }

    public void setRequiresOnClasspath(String requiresOnClasspath) {
        this.requiresOnClasspath = requiresOnClasspath;
    }
	
	
}
