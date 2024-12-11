(ns clojure.gd.assets
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)))

(defn- asset-manager* ^AssetManager []
  (proxy [AssetManager clojure.lang.IFn] []
    (invoke [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn- asset-type->class [k]
  (case k
    :sound   com.badlogic.gdx.audio.Sound
    :texture com.badlogic.gdx.graphics.Texture))

(defn- load-assets [^AssetManager manager assets]
  (doseq [[file class-k] assets]
    (.load manager ^String file (asset-type->class class-k)))
  (.finishLoading manager))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (.list (.internal Gdx/files folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(def folder "resources/")

(def asset-type-exts {:sound   #{"wav"}
                      :texture #{"png" "bmp"}})

(defn setup []
  (def manager (doto (asset-manager*)
                 (load-assets (for [[asset-type exts] asset-type-exts
                                    file (map #(str/replace-first % folder "")
                                              (recursively-search folder exts))]
                                [file asset-type])))))

(defn cleanup []
  (AssetManager/.dispose manager))

(def sound-asset-format "sounds/%s.wav")

(defn play-sound [sound-name]
  (->> sound-name
       (format sound-asset-format)
       manager
       Sound/.play))

(defn all-of-type
  "Returns all asset paths with the specific asset-type."
  [asset-type]
  (filter #(= (.getAssetType manager %) (asset-type->class asset-type))
          (.getAssetNames manager)))
