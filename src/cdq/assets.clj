(ns cdq.assets
  (:require [clojure.string :as str])
  (:import (clojure.lang IFn)
           (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)))

(defn- asset-manager [assets]
  (let [this (proxy [AssetManager IFn] []
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

(defn- recursively-search [folder extensions]
  (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn create [{:keys [folder
                      asset-type->extensions]}]
  (asset-manager
   (for [[asset-type extensions] asset-type->extensions
         file (map #(str/replace-first % folder "")
                   (recursively-search folder extensions))]
     [file asset-type])))

(defn all-of-type [manager asset-type]
  (let [asset-type (case asset-type
                     :sound   Sound
                     :texture Texture)]
    (filter #(= (AssetManager/.getAssetType manager %) asset-type)
            (AssetManager/.getAssetNames manager))))

(defn sound [assets sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       assets))
