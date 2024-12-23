(ns gdl.context.assets
  (:require [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [gdl.assets :as assets]))

(def assets-folder "resources/")

(defn setup []
  (def manager (assets/manager assets-folder)))

(defn cleanup []
  (assets/cleanup manager))

(def sound-asset-format "sounds/%s.wav")

(defn play-sound [sound-name]
  (->> sound-name
       (format sound-asset-format)
       manager
       sound/play))

(defn texture-region [path]
  (texture-region/create (manager path)))

(defn all-of-type [asset-type]
  (assets/all-of-type manager asset-type))
