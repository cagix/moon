(ns forge.assets
  (:require [clojure.gdx :as gdx]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(def ^:private asset-folder "resources/")

(def ^:private asset-manager)

(defn- recursively-search [folder extensions]
  (loop [[^FileHandle file & remaining] (.list (gdx/internal-file folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn- load-all
  "Assets are a collection of vectors `[file class]`.
  All assets are loaded immediately.
  Returns an `com.badlogic.gdx.assets.AssetManager` which supports `get`
  (implements `clojure.lang.ILookup`).

  Has to be disposed."
  [assets]
  (let [manager (proxy [AssetManager clojure.lang.ILookup] []
                  (valAt [^String path]
                    (if (.contains this path)
                      (.get this path)
                      (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))))]
    (doseq [[file class] assets]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defn- search
  "Returns a collection of `[file-path class]` after recursively searching `folder` and matches all `.wav` with `com.badlogic.gdx.audio.Sound` and all `.png`/`.bmp` files with `com.badlogic.gdx.graphics.Texture`."
  [folder]
  (for [[class exts] [[Sound   #{"wav"}]
                      [Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (recursively-search folder exts))]
    [file class]))

(defn- all-of-class
  "Returns all asset paths with the specific class."
  [manager class]
  (filter #(= (.getAssetType manager %)
              class)
          (.getAssetNames manager)))

(defn init []
  (.bindRoot #'asset-manager (load-all (search asset-folder))))

(defn dispose []
  (.dispose asset-manager))

(defn all-textures []
  (all-of-class asset-manager Texture))

(defn all-sounds []
  (all-of-class asset-manager Sound))

(defn play-sound [name]
  (Sound/.play (get asset-manager (str "sounds/" name ".wav"))))

(defn texture-region [path]
  (TextureRegion. ^Texture (get asset-manager path)))
