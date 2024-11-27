(ns forge.assets
  (:require [clojure.gdx.assets :as assets])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(def ^:private manager)

(defn- class-k->class [k]
  (case k
    :sound Sound
    :texture Texture))

(defn init
  "Assets are a collection of vectors `[file class-k]`.
  `class-k` is either :sound or :texture.
  All assets are loaded immediately.
  Has to be disposed."
  [assets]
  (let [manager (assets/manager)]
    (doseq [[file class-k] assets]
      (assets/load manager file (class-k->class class-k)))
    (assets/finish-loading manager)
    (bind-root #'manager manager)))

(defn dispose []
  (.dispose manager))

(defn all-of-class
  "Returns all asset paths with the specific class-k.
  (Either :sound or :texture)."
  [class-k]
  (filter #(= (assets/asset-type manager %)
              (class-k->class class-k))
          (assets/asset-names manager)))

(defn play-sound [name]
  (Sound/.play (get manager (str "sounds/" name ".wav"))))

(defn texture-region [path]
  (TextureRegion. ^Texture (get manager path)))
