public class SimpleSimplexBiomeMapPopulator implements BlendedBiomeProvider.IBiomeMapPopulator {
	OpenSimplex2S[] noises;
	double frequency;
	int octaves;
	int nBiomes;
	
	public SimpleSimplexBiomeMapPopulator(long seed, double frequency, int octaves, int nBiomes) {
		this.noises = new OpenSimplex2S[octaves * nBiomes];
		for (int i = 0; i < this.noises.length; i++) this.noises[i] = new OpenSimplex2S(seed + i);
		this.frequency = frequency;
		this.octaves = octaves;
		this.nBiomes = nBiomes;
	}
	
	public void populateBiomeMap(int regionX, int regionZ, short[] biomeMap) {
		
		// Every point (column) on the region biome map
		for (int rz = 0; rz < BlendedBiomeProvider.PADDED_REGION_SIZE; rz++) {
			int z = (rz - BlendedBiomeProvider.BLEND_RADIUS) + (regionZ << BlendedBiomeProvider.REGION_SIZE_EXPONENT);
			for (int rx = 0; rx < BlendedBiomeProvider.PADDED_REGION_SIZE; rx++) {
				int x = (rx - BlendedBiomeProvider.BLEND_RADIUS) + (regionX << BlendedBiomeProvider.REGION_SIZE_EXPONENT);
				
				// Generate multiple noises. Best value wins.
				// Not the fastest or most controllable biome system, but works for a demo.
				int biome = -1;
				double bestBiomeNoiseValue = Double.NEGATIVE_INFINITY;
				for (int i = 0; i < nBiomes; i++) {
					double noiseValue = getNoise(x, z, i);
					if (noiseValue > bestBiomeNoiseValue) {
						biome = i;
						bestBiomeNoiseValue = noiseValue;
					}
				}
				
				biomeMap[rz * BlendedBiomeProvider.PADDED_REGION_SIZE + rx] = (short) biome;
			}
		}
	}
	
	// fBm on the simplex
	private double getNoise(double x, double z, int biome) {
		double a = 1.0;
		double f = frequency;
		double value = 0.0;
		for (int i = 0; i < octaves; i++) {
			value += noises[biome * octaves + i].noise2(x * f, z * f) * a;
			a *= 0.5;
			f *= 2.0;
			x += i * 123; // Offset the noises a bit to decrease artifacts further.
		}
		return value;
	}
}