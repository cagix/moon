(ns forge.assets
  (:require [clojure.gdx.assets :as assets])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(def ^:private asset-manager)

(defn init
  "Assets are a collection of vectors `[file class]`.
  All assets are loaded immediately.
  Has to be disposed."
  [assets]
  (let [manager (assets/manager)]
    (doseq [[file class] assets]
      (assets/load manager file class))
    (assets/finish-loading manager)
    (bind-root #'asset-manager manager)))

(defn dispose []
  (.dispose asset-manager))

(defn- all-of-class
  "Returns all asset paths with the specific class."
  [manager class]
  (filter #(= (assets/asset-type manager %)
              class)
          (assets/asset-names manager)))

(defn all-textures []
  (all-of-class asset-manager Texture))

(defn all-sounds []
  (all-of-class asset-manager Sound))

(defn play-sound [name]
  (Sound/.play (get asset-manager (str "sounds/" name ".wav"))))

(defn texture-region [path]
  (TextureRegion. ^Texture (get asset-manager path)))
