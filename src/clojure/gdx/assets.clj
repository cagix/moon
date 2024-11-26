(ns clojure.gdx.assets
  (:refer-clojure :exclude [load])
  (:import (com.badlogic.gdx.assets AssetManager)))

(defn manager []
  (proxy [AssetManager clojure.lang.ILookup] []
    (valAt [^String path]
      (if (.contains this path)
        (.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn load [^AssetManager manager file class]
  (.load manager ^String file ^Class class))

(defn finish-loading [^AssetManager manager]
  (.finishLoading manager))

(defn asset-type [^AssetManager manager asset]
  (.getAssetType manager asset))

(defn asset-names [^AssetManager manager]
  (.getAssetNames manager))
