(ns gdl.assets
  (:refer-clojure :exclude [load type])
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

(defn create [assets]
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
