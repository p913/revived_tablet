package ru.revivedtablet;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import ru.revivedtablet.config.Storages;


public class ImageProducer {

	private List<String> files = new ArrayList<String>();
	
	private int width; 
	private int height;
	
	private int currFileIndex = 0;
	
	public ImageProducer () {
		width = TabletCanvasUtils.getDisplayWidth();
		height = TabletCanvasUtils.getDisplayHeight();;
		prepareListOfFiles();
	}
	
	private boolean prepareListOfFiles() {
		currFileIndex = 0;

		files.clear();

		for (Storages.Storage storage : Storages.getInstance().getAvailable()) {
			for (Storages.Folder folder : storage.getFolders())
				for (Storages.Picture picture : folder.getPictures())
					files.add(picture.getPath());
		}

		if (!files.isEmpty()) {
			randomizeFileList();
			return true;
		}
		
		return false;
	}
	
	public Bitmap getNewRandomPreparedImage() {
		if (currFileIndex < files.size()) {
			Bitmap res = ImageUtils.decodeSampledBitmap(
					files.get(currFileIndex++), width, height,
					Math.random() > 0.5 ? ImageUtils.PreferedBoundMode.EQUAL_WITH_BLUR : ImageUtils.PreferedBoundMode.EQUAL_OR_GREAT);
			if (res == null) {
				//Возможно, удалили изображение в постоянной памяти, перестраиваем список
				prepareListOfFiles();
				//Если в списке все равно единственное изображение, то это возможно то же самое, 
				//что мы не смогли загрузить. На всякий случай возвращаем пустое
				if (files.size() <= 1)
					return ImageUtils.createEmptyPicture(width, height);
				else
					return getNewRandomPreparedImage();
			}				
			return res;
		}
		else { 
			//Если дошли до последнего файла с картинками или их не было вовсе
			if (prepareListOfFiles())
				return getNewRandomPreparedImage();
		}
		
		return ImageUtils.createEmptyPicture(width, height);
	}
	
	private void randomizeFileList() {
		if (files.size() < 2)
			return;
		
		int rnd;
		String tmp;
		for (int i = 0; i < files.size(); i++) {
			tmp = files.get(i);
			rnd = (int)(Math.random() * files.size());
			while (rnd == i)
				rnd = (int)(Math.random() * files.size());
			files.set(i, files.get(rnd));
			files.set(rnd, tmp);
		}			
	}


}
