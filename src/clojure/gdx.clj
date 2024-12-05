(ns clojure.gdx
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.graphics.g2d SpriteBatch)))

(defn exit []
  (.exit Gdx/app))

(defn post-runnable [runnable]
  (.postRunnable Gdx/app runnable))

(defn asset-manager ^AssetManager []
  (proxy [AssetManager clojure.lang.IFn] []
    (invoke [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn internal-file [path]
  (.internal Gdx/files path))

(defn sprite-batch []
  (SpriteBatch.))
