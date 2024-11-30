(ns forge.assets
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(declare ^:private manager)

(defn init [folder]
  (bind-root #'manager (proxy [AssetManager clojure.lang.ILookup] []
                         (valAt [^String path]
                           (if (.contains this path)
                             (.get this path)
                             (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))
  (doseq [[class exts] [[Sound   #{"wav"}]
                        [Texture #{"png" "bmp"}]]
          file (map #(str/replace-first % folder "")
                    (recursively-search (.internal Gdx/files folder) exts))]
    (.load manager ^String file ^Class class))
  (.finishLoading manager))

(defn dispose []
  (.dispose manager))

(defn get [asset-path]
  (clojure.core/get manager asset-path))

(defn- all-of-class
  "Returns all asset paths with the specific class."
  [class]
  (filter #(= (.getAssetType manager %) class)
          (.getAssetNames manager)))

(defn all-sounds   [] (all-of-class Sound))
(defn all-textures [] (all-of-class Texture))

(defn play-sound [name]
  (Sound/.play (get (str "sounds/" name ".wav"))))
