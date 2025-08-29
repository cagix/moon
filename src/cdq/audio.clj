(ns cdq.audio
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx Audio
                             Files)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.utils Disposable)))

(defprotocol PAudio
  (all-sounds [_])
  (play-sound! [_ path])
  (dispose! [_]))

(defn create
  [{:keys [audio files]}
   {:keys [sounds]}]
  (let [sounds (into {}
                     (for [path (->> sounds io/resource slurp edn/read-string)]
                       [path (Audio/.newSound audio (Files/.internal files path))]))]
    (reify
      PAudio
      (dispose! [_]
        (run! Disposable/.dispose (vals sounds)))

      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        #_(Sound/.play (get sounds path))))))
