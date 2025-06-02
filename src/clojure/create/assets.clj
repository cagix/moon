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
    [file asset-type]))

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

(defn- k->class ^Class [asset-type-k]
  (case asset-type-k
    :sound Sound ; namespaced k ?
    :texture Texture))

(defn- asset-manager [assets]
  (let [this (AssetManager.)]
    (doseq [[file asset-type-k] assets]
      (.load this ^String file (k->class asset-type-k)))
    (.finishLoading this)
    (reify
      Disposable ; -> here I can reify the protocol stuffs
      (dispose [_]
        (Disposable/.dispose this))

      clojure.lang.IFn
      (invoke [_ path]
        (-> (if (.contains this path)
              (.get this ^String path)
              (throw (IllegalArgumentException. (str "Asset cannot be found: " path))))
            reify-asset))

      assets/Assets
      (all-of-type [_ asset-type-k]
        (filter #(= (.getAssetType this %) (k->class asset-type-k))
                (.getAssetNames this))))))

(defn do! [{:keys [ctx/config
                   ctx/files]
            :as ctx}]
  (assoc ctx :ctx/assets (asset-manager (get-assets-to-load files (:assets config)))))
