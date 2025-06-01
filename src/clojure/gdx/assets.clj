(ns clojure.gdx.assets
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.utils Disposable)))

(defprotocol PAssets
  (all-of-type [_ asset-type]))

(deftype Assets [asset-map]
  PAssets
  (all-of-type [_ asset-type]
    (filter #(= (class %)
                (case asset-type
                  :sound Sound
                  :texture Texture))
            (vals asset-map)))

  clojure.lang.IFn
  (invoke [_ param]
    (asset-map param))

  Disposable
  (dispose [_]
    (println "Disposing assets.")
    (run! Disposable/.dispose (vals asset-map))))

(defn create [assets]
  (->Assets (into {}
                  (for [[file asset-type] assets]
                    [file (case asset-type
                            :sound (.newSound Gdx/audio (.internal Gdx/files file))
                            :texture (Texture. file))]))))

(comment

 ; => Texture is given by the libgdx ' context ' as it uses GL internally global state .....

; (Texture. file)
; ; =>
;
;	public Texture (String internalPath) {
;		this(Gdx.files.internal(internalPath));
;	}
;
;	public Texture (FileHandle file) {
;		this(file, null, false);
;	}
;
;	public Texture (FileHandle file, Format format, boolean useMipMaps) {
;		this(TextureData.Factory.loadFromFile(file, format, useMipMaps));
;	}
;
;  ; => returns TextureData
;
;	public Texture (TextureData data) {
;		this(GL20.GL_TEXTURE_2D, Gdx.gl.glGenTexture(), data);
;	}
;
;	protected Texture (int glTarget, int glHandle, TextureData data) {
;		super(glTarget, glHandle);
;		load(data);
;		if (data.isManaged()) addManagedTexture(Gdx.app, this);
;	}
;
;  ; TextureData.Factory :
;
;		public static TextureData loadFromFile (FileHandle file, Format format, boolean useMipMaps) {
;			if (file == null) return null;
;			if (file.name().endsWith(".cim")) return new FileTextureData(file, PixmapIO.readCIM(file), format, useMipMaps);
;			if (file.name().endsWith(".etc1")) return new ETC1TextureData(file, useMipMaps);
;			if (file.name().endsWith(".ktx") || file.name().endsWith(".zktx")) return new KTXTextureData(file, useMipMaps);
;			return new FileTextureData(file, new Pixmap(file), format, useMipMaps);
;		}
;
;
;    import com.badlogic.gdx.graphics.glutils.FileTextureData;
 )
