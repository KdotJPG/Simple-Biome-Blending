/*
 * Simple Biome Blending Demo
 */

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.*;
import javax.swing.*;

public class Demo
{
	private static final int WIDTH = 512;
	private static final int HEIGHT = 512;
	private static final double NOISE_PERIOD = 192.0;
	private static final int OFF_X = 0;
	private static final int OFF_Y = 0;
	private static final int SEED = 1234;
	private static final int OCTAVES = 3;
	
	private static final double FREQUENCY = 1.0 / NOISE_PERIOD;
	
	private static final Color[] BIOME_COLORS = {
		Color.GREEN, Color.ORANGE.darker(), Color.CYAN.darker()
	};

	public static void main(String[] args)
			throws IOException {
		
		// Initialize
		BlendedBiomeProvider.IBiomeMapPopulator biomeMapPopulator = new SimpleSimplexBiomeMapPopulator(SEED, FREQUENCY, OCTAVES, BIOME_COLORS.length);
		BlendedBiomeProvider blendedBiomeProvider = new BlendedBiomeProvider(biomeMapPopulator);
		
		// Generate, grouped by chunk to save biome region cache queries
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		for (int chunkZ = 0; chunkZ < HEIGHT / BlendedBiomeProvider.CHUNK_SIZE; chunkZ++)
		{
			for (int chunkX = 0; chunkX < WIDTH / BlendedBiomeProvider.CHUNK_SIZE; chunkX++)
			{
				BlendedBiomeProvider.BiomeWeightedIndex[][] chunkBlendedBiomes = blendedBiomeProvider.getBlendedBiomesForChunk(chunkX, chunkZ);
				
				for (int cz = 0; cz < BlendedBiomeProvider.CHUNK_SIZE; cz++) {
					for (int cx = 0; cx < BlendedBiomeProvider.CHUNK_SIZE; cx++) {
						int z = chunkZ * BlendedBiomeProvider.CHUNK_SIZE + cz;
						int x = chunkX * BlendedBiomeProvider.CHUNK_SIZE + cx;
						
						BlendedBiomeProvider.BiomeWeightedIndex[] blendedBiomes = chunkBlendedBiomes[cz * BlendedBiomeProvider.CHUNK_SIZE + cx];
				
						// Blend biome colors together using weights, for visual display.
						float r, g, b; r = g = b = 0;
						for (BlendedBiomeProvider.BiomeWeightedIndex biomeBlendEntry : blendedBiomes) {
							r += BIOME_COLORS[biomeBlendEntry.biome].getRed() * biomeBlendEntry.weight;
							g += BIOME_COLORS[biomeBlendEntry.biome].getGreen() * biomeBlendEntry.weight;
							b += BIOME_COLORS[biomeBlendEntry.biome].getBlue() * biomeBlendEntry.weight;
						}
						
						int rgb = new Color((int)r, (int)g, (int)b).getRGB();
						image.setRGB(x, z, rgb);
					}
				}
			}
		}
		
		// Generate by column
		/*BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < HEIGHT; y++)
		{
			for (int x = 0; x < WIDTH; x++)
			{
				BlendedBiomeProvider.BiomeWeightedIndex[] blendedBiomes = blendedBiomeProvider.getBlendedBiomesAt(x, y);
				
				// Blend biome colors together using weights, for visual display.
				float r, g, b; r = g = b = 0;
				for (BlendedBiomeProvider.BiomeWeightedIndex biomeBlendEntry : blendedBiomes) {
					r += BIOME_COLORS[biomeBlendEntry.biome].getRed() * biomeBlendEntry.weight;
					g += BIOME_COLORS[biomeBlendEntry.biome].getGreen() * biomeBlendEntry.weight;
					b += BIOME_COLORS[biomeBlendEntry.biome].getBlue() * biomeBlendEntry.weight;
				}
				
				int rgb = new Color((int)r, (int)g, (int)b).getRGB();
				image.setRGB(x, y, rgb);
			}
		}*/
		
		// Save it or show it
		if (args.length > 0 && args[0] != null) {
			ImageIO.write(image, "png", new File(args[0]));
			System.out.println("Saved image as " + args[0]);
		} else {
			JFrame frame = new JFrame();
			JLabel imageLabel = new JLabel();
			imageLabel.setIcon(new ImageIcon(image));
			frame.add(imageLabel);
			frame.pack();
			frame.setResizable(false);
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setVisible(true);
		}
		
	}
}