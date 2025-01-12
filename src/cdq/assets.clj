(ns cdq.assets
  (:require [clojure.string :as str]
            [gdl.utils.files :as files])
  (:import (clojure.lang IFn)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)
           (gdl.assets Assets)))

(def ^:private asset-type-class-map
  {:sound   Sound
   :texture Texture})

(defn- asset-type->class [k]
  (get asset-type-class-map k))

(defn- class->asset-type [class]
  (some (fn [[k v]] (when (= v class) k)) asset-type-class-map))

(defn- blocking-load-all [assets]
  (let [manager (proxy [AssetManager IFn Assets] []
                  (invoke [^String path]
                    (let [^AssetManager this this]
                      (if (.contains this path)
                        (.get this path)
                        (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))))
                  (all_of_type [asset-type]
                    (let [^AssetManager manager this]
                      (filter #(= (class->asset-type (.getAssetType manager %)) asset-type)
                              (.getAssetNames manager)))))]
    (doseq [[file asset-type] assets]
      (.load manager ^String file (asset-type->class asset-type)))
    (.finishLoading manager)
    manager))

(defn create [_context {:keys [folder type-exts]}]
  (blocking-load-all
   (for [[asset-type exts] type-exts
         file (map #(str/replace-first % folder "")
                   (files/search-by-extensions folder exts))]
     [file asset-type])))
