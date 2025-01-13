(ns clojure.gdx.assets.manager
  (:import (clojure.lang IFn)
           (com.badlogic.gdx.assets AssetManager)))

(defn create [assets]
  (let [this (proxy [AssetManager IFn] []
               (invoke [^String path]
                 (let [^AssetManager this this]
                   (if (.contains this path)
                     (.get this path)
                     (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))]
    (doseq [[file asset-type] assets]
      (.load this ^String file asset-type))
    (.finishLoading this)
    this))
