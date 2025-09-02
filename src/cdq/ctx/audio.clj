(ns cdq.ctx.audio
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx Audio
                             Files)
           (com.badlogic.gdx.audio Sound)))

(defn create
  [{:keys [audio files]}
   {:keys [sounds path-format]}]
  (into {}
        (for [sound-name (->> sounds io/resource slurp edn/read-string)
              :let [path (format path-format sound-name)]]
          [sound-name
           (Audio/.newSound audio (Files/.internal files path))])))

(defn all-sounds [sounds]
  (map first sounds))

(defn play-sound! [sounds sound-name]
  (assert (contains? sounds sound-name) (str sound-name))
  (Sound/.play (get sounds sound-name)))
