import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

public class BlendedBiomeProvider {
	private static final int CACHE_MAX_ENTRIES = 12;
	
	public static final int REGION_SIZE_EXPONENT = 7; // SIZExSIZE, SIZE=2^EXPONENT; 2^7=128
	public static final int CHUNK_SIZE_EXPONENT = 4; // SIZExSIZE, SIZE=2^EXPONENT; 2^4=16
	public static final int BLEND_RADIUS = 16;
	
	public static final int REGION_SIZE = 1 << REGION_SIZE_EXPONENT;
	public static final int CHUNK_SIZE = 1 << CHUNK_SIZE_EXPONENT;
	public static final int PADDED_REGION_SIZE = REGION_SIZE + BLEND_RADIUS*2;
	
	private IBiomeMapPopulator biomeMapPopulator;
	private LinkedList<BiomeMapEntry> biomeMapCache;
	
	public BlendedBiomeProvider(IBiomeMapPopulator biomeMapPopulator) {
		this.biomeMapPopulator = biomeMapPopulator;
		biomeMapCache = new LinkedList<BiomeMapEntry>();
	}
	
	// Get the blended biomes for a single column of the world.
	public BiomeWeightedIndex[] getBlendedBiomesAt(int x, int z) {
		int regionX = x >> REGION_SIZE_EXPONENT;
		int regionZ = z >> REGION_SIZE_EXPONENT;
		short[] biomeMap = getBiomeMapFor(regionX, regionZ);
		
		return getBlendedBiomesFromRegion(x, z, biomeMap);
	}
	
	// If a chunk size is (1) a power of two, and (2) smaller or equal in size to the region size,
	// then it is contained entirely in one region, so we only need one region cache query.
	public BiomeWeightedIndex[][] getBlendedBiomesForChunk(int chunkX, int chunkZ) {
		int regionX = chunkX >> (REGION_SIZE_EXPONENT - CHUNK_SIZE_EXPONENT);
		int regionZ = chunkZ >> (REGION_SIZE_EXPONENT - CHUNK_SIZE_EXPONENT);
		short[] biomeMap = getBiomeMapFor(regionX, regionZ);
		
		BiomeWeightedIndex[][] chunkResults = new BiomeWeightedIndex[CHUNK_SIZE * CHUNK_SIZE][];
		for (int cz = 0; cz < CHUNK_SIZE; cz++) {
			for (int cx = 0; cx < CHUNK_SIZE; cx++) {
				chunkResults[cz * CHUNK_SIZE + cx] = getBlendedBiomesFromRegion(
					(chunkX << CHUNK_SIZE_EXPONENT) + cx,
					(chunkZ << CHUNK_SIZE_EXPONENT) + cz,
					biomeMap);
			}
		}
		
		return chunkResults;
	}
	
	private BiomeWeightedIndex[] getBlendedBiomesFromRegion(int x, int z, short[] biomeMap) {
		ArrayList<BiomeWeightedIndex> results = new ArrayList<BiomeWeightedIndex>(3);
		
		// Mod the world coordinate by the region size.
		int xMasked = (x & (REGION_SIZE - 1));
		int zMasked = (z & (REGION_SIZE - 1));
		
		// Debug: Return single blended biome
		/*short centerBiome = biomeMap[(zMasked + BLEND_RADIUS) * PADDED_REGION_SIZE + (xMasked + BLEND_RADIUS)];
		BiomeWeightedIndex singleIndex = new BiomeWeightedIndex();
		singleIndex.weight = 1.0f;
		singleIndex.biome = centerBiome;
		if (true) return new BiomeWeightedIndex[] { singleIndex };*/
		
		// Loop over every point in the square containing the circle representing the blending radius.
		for (int iz = 0; iz < BLEND_RADIUS*2+1; iz++) {
			int idz = iz - BLEND_RADIUS;
			for (int ix = 0; ix < BLEND_RADIUS*2+1; ix++) {
				int idx = ix - BLEND_RADIUS;
				
				// Weight of the blur kernel over this point
				float thisWeight = BLUR_KERNEL[iz * (BLEND_RADIUS*2+1) + ix];
				if (thisWeight <= 0) continue; // We can skip when it's zero
				
				// Biome at this square within the blending circle
				short thisBiome = biomeMap[(zMasked + iz) * PADDED_REGION_SIZE + xMasked + ix];
				
				// If we've already seen this biome at least once, keep adding to its weight
				boolean foundEntry = false;
				for (BiomeWeightedIndex entry : results) {
					if (entry.biome == thisBiome) {
						entry.weight += thisWeight;
						foundEntry = true;
						break;
					}
				}
				
				// Otherwise create a new results entry.
				if (!foundEntry) {
					BiomeWeightedIndex entry = new BiomeWeightedIndex();
					entry.biome = thisBiome;
					entry.weight = thisWeight;
					results.add(entry);
				}
			}
		}
		
		return results.toArray(new BiomeWeightedIndex[results.size()]);
	}
	
	// Querys the cache, or generates and adds to cache.
	public short[] getBiomeMapFor(int regionX, int regionZ) {
		
		// Is it in the cache?
		BiomeMapEntry correctCacheEntry = null;
		for (ListIterator<BiomeMapEntry> it = biomeMapCache.listIterator(); it.hasNext(); ) {
			BiomeMapEntry cacheEntry = it.next();
			
			// Is this cache entry what we want?
			if (cacheEntry.regionX == regionX && cacheEntry.regionZ == regionZ) {
				
				// It's in the cache, remove and assign it to a variable.
				// We will re-add it to the list in a bit, at the front.
				correctCacheEntry = cacheEntry;
				it.remove();
				break;
			}
		}
		
		// If it wasn't in the cache, we need to create it.
		if (correctCacheEntry == null) {
			System.out.println("Cache miss: " + regionX + "," + regionZ);
			correctCacheEntry = new BiomeMapEntry();
			correctCacheEntry.regionX = regionX;
			correctCacheEntry.regionZ = regionZ;
			correctCacheEntry.map = new short[PADDED_REGION_SIZE * PADDED_REGION_SIZE];
			biomeMapPopulator.populateBiomeMap(regionX, regionZ, correctCacheEntry.map);
		}
		
		// Add (or re-add) it, as the first entry.
		biomeMapCache.addFirst(correctCacheEntry);
		
		// If the cache exceeds the maximum size, remove the last entry.
		if (biomeMapCache.size() > CACHE_MAX_ENTRIES) {
			biomeMapCache.removeLast();
		}
		
		return correctCacheEntry.map;
	}
	
	private static class BiomeMapEntry {
		short[] map;
		int regionX, regionZ;
	}
	
	public static class BiomeWeightedIndex {
		short biome;
		float weight;
	}
	
	public interface IBiomeMapPopulator {
		void populateBiomeMap(int regionX, int regionZ, short[] biomeMap);
	}
	
	// Precompute the blending kernel
	// We use a blending kernel reminiscent of Gaussian blur, but with a continuous derivative at the truncation.
	private static final float[] BLUR_KERNEL = new float[(BLEND_RADIUS*2+1) * (BLEND_RADIUS*2+1)];
	static {
		float weightTotal = 0.0f;
		for (int iz = 0; iz < BLEND_RADIUS*2+1; iz++) {
			int idz = iz - BLEND_RADIUS;
			for (int ix = 0; ix < BLEND_RADIUS*2+1; ix++) {
				int idx = ix - BLEND_RADIUS;
				float thisWeight = (BLEND_RADIUS * BLEND_RADIUS - idx * idx - idz * idz);
				if (thisWeight <= 0) continue; // We only compute for the circle of positive values of the blending function.
				thisWeight *= thisWeight; // Make transitions smoother.
				weightTotal += thisWeight;
				BLUR_KERNEL[iz * (BLEND_RADIUS*2+1) + ix] = thisWeight;
			}
		}
		
		// Rescale the weights, so they all add up to 1.
		for (int i = 0; i < BLUR_KERNEL.length; i++) BLUR_KERNEL[i] /= weightTotal;
	}
}