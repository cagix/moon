(ns moon.assets
  (:refer-clojure :exclude [get load])
  (:import (com.badlogic.gdx.assets AssetManager)))

(declare ^:private ^AssetManager manager)

(defn load
  "Assets are a collection of vectors [file class]"
  [assets]
  (.bindRoot #'manager (AssetManager.))
  (doseq [[file class] assets]
    (.load manager ^String file ^Class class))
  (.finishLoading manager))

(defn get
  "Gets the asset at file-path."
  [path]
  (.get manager ^String path))

(defn dispose
  "Frees all resources of loaded assets."
  []
  (.dispose manager))

(defn of-class
  "Returns all asset paths with the specific class."
  [class]
  (filter #(= (.getAssetType manager %)
              class)
          (.getAssetNames manager)))
