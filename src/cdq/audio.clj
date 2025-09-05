(ns cdq.audio
  (:require [clojure.edn :as edn]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.files :as files]
            [clojure.java.io :as io]))

(defn create
  [audio files {:keys [sounds path-format]}]
  (into {}
        (for [sound-name (->> sounds io/resource slurp edn/read-string)
              :let [path (format path-format sound-name)]]
          [sound-name
           (audio/sound audio (files/internal files path))])))

(defn all-sounds [sounds]
  (map first sounds))

(defn play-sound! [sounds sound-name]
  (assert (contains? sounds sound-name) (str sound-name))
  (sound/play! (get sounds sound-name)))
