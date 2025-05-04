(ns cdq.assets
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.assets AssetManager)
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

(declare ^:private ^AssetManager asset-manager)

(defn create! [{:keys [folder asset-type->extensions]}]
  (let [manager (AssetManager.)]
    (doseq [[file asset-type] (for [[asset-type extensions] asset-type->extensions
                                    file (map #(str/replace-first % folder "")
                                              (recursively-search (.internal Gdx/files folder) extensions))]
                                [file asset-type])]
      (.load manager ^String file (case asset-type
                                    :sound Sound
                                    :texture Texture)))
    (.finishLoading manager)
    (.bindRoot #'asset-manager manager)))

(defn dispose! []
  (.dispose asset-manager))

(defn all-of-type [asset-type]
  (let [asset-type (case asset-type
                     :sound   Sound
                     :texture Texture)]
    (filter #(= (.getAssetType asset-manager %) asset-type)
            (.getAssetNames asset-manager))))

(defn get [^String path]
  (if (.contains asset-manager path)
    (.get asset-manager path)
    (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))

(defn sound [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       get))
