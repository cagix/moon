(ns gdl.assets
  (:refer-clojure :exclude [load type])
  (:require [clojure.files :as files]
            [clojure.files.file-handle :as fh]
            [clojure.string :as str])
  (:import (clojure.lang IFn)
           (com.badlogic.gdx.assets AssetManager)))

; TODO extend for all supported & document ?
; music, tiled-maps,etc. also possible ...
(def ^:private asset-type-class-map
  {:sound   com.badlogic.gdx.audio.Sound
   :texture com.badlogic.gdx.graphics.Texture})

(defn- asset-type->class [k]
  (get asset-type-class-map k))

(defn- class->asset-type [class]
  (some (fn [[k v]] (when (= v class) k)) asset-type-class-map))

(defn- create-manager [assets]
  (let [manager (proxy [AssetManager IFn] []
                  (invoke [^String path]
                    (let [^AssetManager this this]
                      (if (.contains this path)
                        (.get this path)
                        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))]
    (doseq [[file asset-type] assets]
      (.load manager ^String file (asset-type->class asset-type)))
    (.finishLoading manager)
    manager))

(defn all-of-type [assets asset-type]
  (let [^AssetManager manager assets]
    (filter #(= (class->asset-type (.getAssetType manager %)) asset-type)
            (.getAssetNames manager))))

(defn- search-by-extensions [folder extensions]
  (loop [[file & remaining] (fh/list folder)
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(defn- search-assets [files folder]
  (for [[asset-type exts] {:sound   #{"wav"}
                           :texture #{"png" "bmp"}}
        file (map #(str/replace-first % folder "")
                  (search-by-extensions (files/internal files folder)
                                        exts))]
    [file asset-type]))

(defn search-and-load [files folder]
  (create-manager (search-assets files folder)))
