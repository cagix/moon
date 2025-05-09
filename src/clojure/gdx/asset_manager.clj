(ns clojure.gdx.asset-manager
  (:refer-clojure :exclude [get])
  (:import (com.badlogic.gdx.assets AssetManager)))

(defn create [assets]
  (let [manager (AssetManager.)]
    (doseq [[file asset-type] assets]
      (.load manager ^String file ^Class asset-type))
    (.finishLoading manager)
    manager))

(defn all-of-type [^AssetManager asset-manager asset-type]
  (filter #(= (.getAssetType asset-manager %) asset-type)
          (.getAssetNames asset-manager)))

(defn get [^AssetManager asset-manager ^String path]
  (if (.contains asset-manager path)
    (.get asset-manager path)
    (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))
