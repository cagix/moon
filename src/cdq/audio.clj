(ns cdq.audio
  (:require [clojure.gdx :as gdx])
  (:import (com.badlogic.gdx.audio Sound)))

(defn create [gdx sound-names path-format]
  (into {}
        (for [sound-name sound-names]
          [sound-name
           (->> sound-name
                (format path-format)
                (gdx/sound gdx))])))

(defn sound-names [sounds]
  (map first sounds))

(defn play! [sounds sound-name]
  (assert (contains? sounds sound-name) (str sound-name))
  (Sound/.play (get sounds sound-name)))

(defn dispose! [sounds]
  (run! Sound/.dispose (vals sounds)))
