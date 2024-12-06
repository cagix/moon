(ns clojure.gdx.asset-manager
  (:refer-clojure :exclude [load])
  (:import (com.badlogic.gdx.assets AssetManager)))

(defn create ^AssetManager []
  (proxy [AssetManager clojure.lang.IFn] []
    (invoke [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn- class-k->class ^Class [k]
  (case k
    :texture com.badlogic.gdx.graphics.Texture
    :sound   com.badlogic.gdx.audio.Sound))

(defn load [^AssetManager manager assets]
  (doseq [[file asset-type] assets]
    (.load manager ^String file (class-k->class asset-type)))
  (.finishLoading manager))

(defn all-of-class
  "Returns all asset paths with the specific asset-type."
  [^AssetManager manager asset-type]
  (let [class (class-k->class asset-type)]
    (filter #(= (.getAssetType manager %) class)
            (.getAssetNames manager))))
