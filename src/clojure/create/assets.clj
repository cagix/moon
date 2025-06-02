(ns clojure.create.assets
  (:require [clojure.assets :as assets]
            [clojure.audio.sound :as sound]
            [clojure.files :as files]
            [clojure.files.file-handle :as fh]
            [clojure.graphics.texture :as texture]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.assets AssetManager)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
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

(defn- get-assets-to-load [files
                           {:keys [folder
                                   asset-type-extensions]}]
  (for [[asset-type extensions] asset-type-extensions
        file (map #(str/replace-first % folder "")
                  (recursively-search (files/internal files folder)
                                      extensions))]
    [file (case asset-type
            :sound Sound
            :texture Texture)]))

(defn- create-manager [assets]
  (let [manager (AssetManager.)]
    (doseq [[file class] assets]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defn safe-get [^AssetManager this path]
  (if (.contains this path)
    (.get this ^String path)
    (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))

(defn all-of-class [^AssetManager assets class]
  (filter #(= (.getAssetType assets %) class)
          (.getAssetNames assets)))

; no wait -> clojure.gdx.assets.manager reifies clojure.graphics.texture/clojure.audio.sound ....
; so we only pass assets-to-load ...
; 'gdx/asset-manager' ?
; its already IFn ... and implements clojure.assets.manager/all-of-class & disposable
; which I can make again as protocol ... !?
(defn- reify-asset [asset]
  (if (instance? Sound asset)
    (reify sound/Sound
      (play! [_]
        (Sound/.play asset)))
    (reify texture/Texture
      (region [_]
        (TextureRegion. ^Texture asset))
      (region [_ x y w h]
        (TextureRegion. ^Texture asset
                        (int x)
                        (int y)
                        (int w)
                        (int h))))))

(defn- create-assets [files config]
  (let [manager (create-manager (get-assets-to-load files config))]
    (reify
      Disposable
      (dispose [_]
        (Disposable/.dispose manager))

      clojure.lang.IFn
      (invoke [_ path]
        (reify-asset (safe-get manager path)))

      assets/Assets
      (all-sounds [_]
        (all-of-class manager Sound))

      (all-textures [_]
        (all-of-class manager Texture)))))

(defn do! [{:keys [ctx/config
                   ctx/files]
            :as ctx}]
  (assoc ctx :ctx/assets (create-assets files (:assets config))))
