(ns cdq.assets
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.utils Disposable)))

(defn- asset-manager [assets]
  (let [manager (AssetManager.)]
    (doseq [[file asset-type] assets]
      (.load manager ^String file ^Class asset-type))
    (.finishLoading manager)
    manager))

(defn- safe-get [^AssetManager asset-manager ^String path]
  (if (.contains asset-manager path)
    (.get asset-manager path)
    (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))

(defn- all-of-type* [^AssetManager asset-manager asset-type]
  (filter #(= (.getAssetType asset-manager %) asset-type)
          (.getAssetNames asset-manager)))

(defn- recursively-search [^FileHandle folder extensions]
  (loop [[^FileHandle file & remaining] (.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn- search [folder]
  (for [[asset-type extensions] {com.badlogic.gdx.audio.Sound      #{"wav"}
                                 com.badlogic.gdx.graphics.Texture #{"png" "bmp"}}
        file (map #(str/replace-first % folder "")
                  (recursively-search (.internal Gdx/files folder) extensions))]
    [file asset-type]))

(defprotocol Assets
  (all-of-type [_ asset-type]))

(defn create [folder]
  (let [this (asset-manager (search folder))]
    (reify clojure.lang.IFn
      (invoke [_ path]
        (safe-get this path))
      Assets
      (all-of-type [_ asset-type]
        (all-of-type* this asset-type))
      Disposable
      (dispose [_]
        (Disposable/.dispose this)))))
