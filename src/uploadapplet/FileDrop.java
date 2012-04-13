package uploadapplet;

import java.io.*;
import java.util.List;
import java.awt.datatransfer.*;
import javax.swing.*;

public class FileDrop{

}

@SuppressWarnings("serial")
class FileDropHandler extends TransferHandler{
	public boolean canImport(TransferSupport sup){
		if(sup.isDataFlavorSupported(DataFlavor.javaFileListFlavor) == false){
			return false;
		}
		
		return true;
	}
	public boolean importData(TransferSupport sup){
		int index;
		
		Transferable trans;
		List<File> fileList;
		
		try{
			trans = sup.getTransferable();
			fileList = (List<File>)trans.getTransferData(DataFlavor.javaFileListFlavor);
			for(index = 0;index < fileList.size();index++){
				UploadApplet.addUpload(fileList.get(index));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return true;
	}
}
