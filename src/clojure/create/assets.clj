(ns clojure.create.assets
  (:require [clojure.assets :as assets]
            [clojure.audio.sound :as sound]
            [clojure.files :as files]
            [clojure.files.file-handle :as fh]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.utils Disposable)))

(defn- recursively-search [folder extensions]
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

(defn- create-manager [assets]
  (let [manager (AssetManager.)]
    (doseq [[file class] assets]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defn- safe-get [^AssetManager this path]
  (if (.contains this path)
    (let [asset (.get this ^String path)]
      (if (= (.getAssetType this path) Sound)
        (reify sound/Sound
          (play! [_]
            (Sound/.play asset)))
        asset))
    (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))

(defn- all-of-type [^AssetManager assets class]
  (filter #(= (.getAssetType assets %) class)
          (.getAssetNames assets)))

(defn- create-assets [files {:keys [folder asset-type-extensions]}]
  (let [manager (create-manager
                 (for [[asset-type extensions] asset-type-extensions
                       file (map #(str/replace-first % folder "")
                                 (recursively-search (files/internal files folder)
                                                     extensions))]
                   [file (case asset-type
                           :sound Sound
                           :texture Texture)]))]
    (reify
      Disposable
      (dispose [_]
        (Disposable/.dispose manager))

      clojure.lang.IFn
      (invoke [_ path]
        (safe-get manager path))

      assets/Assets
      (all-sounds [_]
        (all-of-type manager Sound))

      (all-textures [_]
        (all-of-type manager Texture)))))

(defn do! [{:keys [ctx/config
                   ctx/files]
            :as ctx}]
  (assoc ctx :ctx/assets (create-assets files (:assets config))))
