
package pt.up.fe.labtablet.models.Dendro.Ontologies;

import java.util.List;

public class Ddr{
   	private String handle;
   	private String rootFolder;
    private String fileExtension;

    public String getFileExtension(){
        return this.fileExtension;
    }
    public void setFileExtension(String fileExtension){
        this.fileExtension = fileExtension;
    }

 	public String getHandle(){
		return this.handle;
	}
	public void setHandle(String handle){
		this.handle = handle;
	}
 	public String getRootFolder(){
		return this.rootFolder;
	}
	public void setRootFolder(String rootFolder){
		this.rootFolder = rootFolder;
	}
}