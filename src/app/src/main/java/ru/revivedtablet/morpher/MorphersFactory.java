package ru.revivedtablet.morpher;

import java.util.ArrayList;
import java.util.List;

public class MorphersFactory {
	private static MorphersFactory instance;
	
	private List<ImageMorpher> morphers; 

	private MorphersFactory() {
		morphers = new ArrayList<ImageMorpher>();
		
		morphers.add(new HorizontalLinesMorpher());
		morphers.add(new VerticalLinesMorpher());
		morphers.add(new ColorMixMorpher());
	}
	
	public static MorphersFactory getInstance() {
		if (instance == null)
			instance = new MorphersFactory();
		return instance;		
	}
	
	public List<ImageMorpher> getMorphers() {
		return morphers;
	}

	public ImageMorpher getRandomMorpher() {
		return morphers.get((int)(Math.random() * morphers.size()));
	}
	
}
