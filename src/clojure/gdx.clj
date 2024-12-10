(ns clojure.gdx
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color Colors Texture Pixmap Pixmap$Format OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion)
           (com.badlogic.gdx.utils Disposable ScreenUtils)))

(def dispose Disposable/.dispose)

(defn static-field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str/replace (str/upper-case (name k)) "-" "_")))))

(def ^:private k->input-button (partial static-field "Input$Buttons"))
(def ^:private k->input-key    (partial static-field "Input$Keys"))

(defn button-just-pressed? [b]
  (.isButtonJustPressed Gdx/input (k->input-button b)))

(defn key-just-pressed? [k]
  (.isKeyJustPressed Gdx/input (k->input-key k)))

(defn key-pressed? [k]
  (.isKeyPressed Gdx/input (k->input-key k)))

(def ^Color black Color/BLACK)
(def ^Color white Color/WHITE)

(defn ->color
  ([r g b]
   (->color r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a)))
  (^Color [c]
          (cond (= Color (class c)) c
                (keyword? c) (static-field "graphics.Color" c)
                (vector? c) (apply ->color c)
                :else (throw (ex-info "Cannot understand color" c)))))

(defn add-color [name-str color]
  (Colors/put name-str (->color color)))

(defn exit []
  (.exit Gdx/app))

(defmacro post-runnable [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(defn- asset-manager* ^AssetManager []
  (proxy [AssetManager clojure.lang.IFn] []
    (invoke [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn- class-k->class ^Class [k]
  (case k
    :texture com.badlogic.gdx.graphics.Texture
    :sound   com.badlogic.gdx.audio.Sound))

(defn- load-assets [^AssetManager manager assets]
  (doseq [[file asset-type] assets]
    (.load manager ^String file (class-k->class asset-type)))
  (.finishLoading manager))

(defn asset-manager [assets]
  (doto (asset-manager*)
    (load-assets assets)))

(defn all-of-class
  "Returns all asset paths with the specific asset-type."
  [^AssetManager manager asset-type]
  (let [class (class-k->class asset-type)]
    (filter #(= (.getAssetType manager %) class)
            (.getAssetNames manager))))

(defn internal ^FileHandle [path]
  (.internal Gdx/files path))

(defn recursively-search [folder extensions]
  (loop [[file & remaining] (.list (internal folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn frames-per-second []
  (.getFramesPerSecond Gdx/graphics))

(defn delta-time []
  (.getDeltaTime Gdx/graphics))

(defn cursor [pixmap hotspot-x hotspot-y]
  (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y))

(defn set-cursor [cursor]
  (.setCursor Gdx/graphics cursor))

(defn pixmap
  (^Pixmap [^FileHandle file-handle]
   (Pixmap. file-handle))
  (^Pixmap [width height]
   (Pixmap. (int width) (int height) Pixmap$Format/RGBA8888)))

(defn orthographic-camera ^OrthographicCamera []
  (OrthographicCamera.))

(defn clear-screen [color]
  (ScreenUtils/clear color))

(defn texture [^Pixmap pixmap]
  (Texture. pixmap))

(defn texture-region
  ([^Texture texture]
   (TextureRegion. texture))
  ([^Texture texture x y w h]
   (TextureRegion. texture (int x) (int y) (int w) (int h))))

(defn ->texture-region [^TextureRegion texture-region x y w h]
  (TextureRegion. texture-region (int x) (int y) (int w) (int h)))

(defn region-width [texture-region]
  (TextureRegion/.getRegionWidth texture-region))

(defn region-height [texture-region]
  (TextureRegion/.getRegionHeight texture-region))

(defn sprite-batch []
  (SpriteBatch.))
