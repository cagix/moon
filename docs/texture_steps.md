- your game should consist of simple independent as context-free as much parts -
    - those are your namespaces at the bottom ? -


0. -> also how are we using the textures which come from ':ctx/assets' ?

    1. - clojure.graphics.texture/region (assets texture-path)
        -> clojure.graphics/create-sprite

            1.1 regionWidth / regionHeight
            1.2 draw-texture-region!
                .draw batch texture-region -> SpriteBatch -> works well why should we port it???

                    => maybe just pass 'Gdx' through with a fork of libgdx
                        -> so I know e.g. Texture is part of graphics
                            -> and can just make (ctx/texture asdf "foobar")
                                -> But I am doing this already thoguh the ctx/assets ???
                                    -> so there is only one step the asset-manager which is weird
                                        -> but it works so

    2. tiled/static-tiled-map-tile texture-region

    3. clojure.ui.dev-menu -> :icon -> ui/image-widget -> TextureRegion.

        Wait ---
            just texture/region with both arities .....
            * :icon @ clojure.ui.menu also ... should work ...

1. (Texture. path)

2.  this(Gdx.files.internal(internalPath));

3.  this(file, null, false);

4.
	public Texture (FileHandle file, Format format, boolean useMipMaps) {
		this(TextureData.Factory.loadFromFile(file, format, useMipMaps));
	}

5.

		public static TextureData loadFromFile (FileHandle file, Format format, boolean useMipMaps) {
			if (file == null) return null;
			if (file.name().endsWith(".cim")) return new FileTextureData(file, PixmapIO.readCIM(file), format, useMipMaps);
			if (file.name().endsWith(".etc1")) return new ETC1TextureData(file, useMipMaps);
			if (file.name().endsWith(".ktx") || file.name().endsWith(".zktx")) return new KTXTextureData(file, useMipMaps);
			return new FileTextureData(file, new Pixmap(file), format, useMipMaps);
		}

6.
			return new FileTextureData(file, new Pixmap(file), format, useMipMaps);

7. @ com.badlogic.gdx.graphics.glutils.FileTextureData implements TextureData

	public FileTextureData (FileHandle file, Pixmap preloadedPixmap, Format format, boolean useMipMaps) {
		this.file = file;
		this.pixmap = preloadedPixmap;
		this.format = format;
		this.useMipMaps = useMipMaps;
		if (pixmap != null) {
			width = pixmap.getWidth();
			height = pixmap.getHeight();
			if (format == null) this.format = pixmap.getFormat();
		}
	}

8. so back to step 7: new Pixmap(file)

	/** Creates a new Pixmap instance from the given file. The file must be a Png, Jpeg or Bitmap. Paletted formats are not
	 * supported.
	 *
	 * @param file the {@link FileHandle} */
	public Pixmap (FileHandle file) {
		try {
			byte[] bytes = file.readBytes();
			pixmap = new Gdx2DPixmap(bytes, 0, bytes.length, 0);
		} catch (Exception e) {
			throw new GdxRuntimeException("Couldn't load file: " + file, e);
		}
	}

9. here we are finally at native Gdx-context-free code:

	public Gdx2DPixmap (byte[] encodedData, int offset, int len, int requestedFormat) throws IOException {
		pixelPtr = load(nativeData, encodedData, offset, len);
		if (pixelPtr == null) throw new IOException("Error loading pixmap: " + getFailureReason());

		basePtr = nativeData[0];
		width = (int)nativeData[1];
		height = (int)nativeData[2];
		format = (int)nativeData[3];

		if (requestedFormat != 0 && requestedFormat != format) {
			convert(requestedFormat);
		}
	}
