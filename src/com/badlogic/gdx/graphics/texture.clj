(ns com.badlogic.gdx.graphics.texture
  (:import (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture
                                      Pixmap)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn from-file [file-handle]
  (Texture. ^FileHandle file-handle)

;	public Texture (FileHandle file) {
;		this(file, null, false);
;	}

;	public Texture (FileHandle file, Format format, boolean useMipMaps) {
;		this(TextureData.Factory.loadFromFile(file, format, useMipMaps));
;	}

;	public Texture (TextureData data) {
;		this(GL20.GL_TEXTURE_2D, Gdx.gl.glGenTexture(), data);
;}
  )

(defn create [pixmap]
  (Texture. ^Pixmap pixmap)

;	public Texture (Pixmap pixmap) {
;		this(new PixmapTextureData(pixmap, null, false, false));
;	}
;	public Texture (TextureData data) {
;		this(GL20.GL_TEXTURE_2D, Gdx.gl.glGenTexture(), data);
;}
  )
; TODO but also using 'Gdx/app'
; so '(clojure.gdx/texture gdx file-handle) '?

;	protected Texture (int glTarget, int glHandle, TextureData data) {
;		super(glTarget, glHandle);
;		load(data);
;		if (data.isManaged()) addManagedTexture(Gdx.app, this);
;	}


; Just Texture & some data,
; can create myself?
(defn region
  ([^Texture texture]
   (TextureRegion. texture))
  ([^Texture texture [x y w h]]
   (TextureRegion. texture
                   (int x)
                   (int y)
                   (int w)
                   (int h)))
  ([^Texture texture x y w h]
   (TextureRegion. texture
                   (int x)
                   (int y)
                   (int w)
                   (int h))))
