(ns clojure.gdx.assets
  (:import (clojure.lang IFn)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)))

(defn create [assets]
  (let [this (proxy [AssetManager IFn] [] ; not proxy but only reify/protocol!
               (invoke [^String path]
                 (let [^AssetManager this this]
                   (if (.contains this path)
                     (.get this path)
                     (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))]
    (doseq [[file asset-type] assets]
      (.load this ^String file (case asset-type
                                 :sound Sound
                                 :texture Texture)))
    (.finishLoading this)
    this))

(defn all-of-type [manager asset-type]
  (let [asset-type (case asset-type
                     :sound   Sound
                     :texture Texture)]
    (filter #(= (AssetManager/.getAssetType manager %) asset-type)
            (AssetManager/.getAssetNames manager))))
