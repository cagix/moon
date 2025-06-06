(ns clojure.gdx.assets.manager
  (:import (com.badlogic.gdx.assets AssetManager)))

(defn create [assets]
  (let [this (AssetManager.)]
    (doseq [[file class] assets]
      (.load this ^String file ^Class class))
    (.finishLoading this)
    this))

(defn dispose! [^AssetManager this]
  (.dispose this))

(defn safe-get [^AssetManager this path]
  (.get this ^String path true))

(defn all-of-class [^AssetManager this class]
  (filter #(= (.getAssetType this %) class)
          (.getAssetNames this)))
