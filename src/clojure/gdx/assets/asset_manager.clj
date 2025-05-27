(ns clojure.gdx.assets.asset-manager
  (:import (com.badlogic.gdx.assets AssetManager)))

(defn create [assets]
  (let [manager (AssetManager.)]
    (doseq [[file class] assets]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defn safe-get [^AssetManager this path]
  (if (.contains this path)
    (.get this ^String path)
    (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))

(defn all-of-type [^AssetManager assets class]
  (filter #(= (.getAssetType assets %) class)
          (.getAssetNames assets)))
