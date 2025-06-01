(ns gdl.assets
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.files :as files]
            [clojure.string :as str]
            [gdl.audio.sound])
  (:import (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)))

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

(defn- create-asset-manager [assets]
  (let [manager (AssetManager.)]
    (doseq [[file class] assets]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defn- safe-get [^AssetManager this path]
  (if (.contains this path)
    (.get this ^String path)
    (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))

(defn- all-of-type [^AssetManager assets class]
  (filter #(= (.getAssetType assets %) class)
          (.getAssetNames assets)))

(defn create [{:keys [folder asset-type-extensions]}]
  (create-asset-manager
   (for [[asset-type extensions] asset-type-extensions
         file (map #(str/replace-first % folder "")
                   (recursively-search (files/internal (gdx/files) folder) extensions))]
     [file (case asset-type
             :sound Sound
             :texture Texture)])))

(defn sound [assets path]
  (let [sound (safe-get assets path)]
    (reify gdl.audio.sound/Sound
      (play! [_]
        (Sound/.play sound)))))

(defn texture [assets path]
  (safe-get assets path))

(defn all-sounds [assets]
  (all-of-type assets Sound))

(defn all-textures [assets]
  (all-of-type assets Texture))
