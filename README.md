# Simple Biome Blending Demo

A simple biome blender class, using a region cache and a blur kernel. The demo blends colors to produce an image, but the blender is intended to be used to smooth together the parameters or noise values of neighboring biomes, in order to produce a seamless transition. The output takes the form of an array of weighted indices `{ short biomeIndex, float weight }`, where the weights all add up to one. The blending radius, chunk size, and cache region size are variable. Here, region refers to a large square of the world (e.g. 128x128) which may cover many chunks.

### Features

- Biome source agnostic. Any generator that can populate an NxN region with biomes (where N is a power of two plus padding), such that each region is part of the larger picture, can be adapted.
- Constant blending distance. The travel distance over each transition border in the world should be roughly the same.

### Limitations

- Larger blending radii increase the generation time quadratically.
- Current version not threadsafe when multiple chunks are generated on different threads.
- No opportunity for performance enhancement where it is certain only one biome is in range of the blending kernel.
- Included biome placement is for demonstration and does not attempt to avoid certain biomes bordering others (e.g. a desert and a snowy tundra).

### Image

![Demo Image](images/demo.png?raw=true)

### Notes

This demo uses a Simplex-type noise for its biome placement. Good Simplex-type noise implementations tend to produce fewer visible grid alignment artifacts than older Perlin noise. I would venture to guess that most tutorials and projects which have used Perlin, did so either because because the author heard about it first, or because it was convenient in a particular library. I believe that most of them would have produced better results by using a Simplex-type noise. I have created [OpenSimplex/OpenSimplex2](https://github.com/KdotJPG/OpenSimplex2) to address IP claims as well as strong diagonal artifacts, which may be of concern with other noise implementations in the Simplex category.

This demo is released under the Public Domain Unlicense, in hopes that it may serve as many people as possible. Credit is appreciated but certainly not required.