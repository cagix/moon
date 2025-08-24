(ns cdq.create.audio
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cdq.audio])
  (:import (com.badlogic.gdx Audio
                             Files)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.utils Disposable)))

(defn do!
  [{:keys [ctx/audio
           ctx/files]}
   {:keys [sounds]}]
  (let [sounds (into {}
                     (for [path (->> sounds io/resource slurp edn/read-string)]
                       [path (Audio/.newSound audio (Files/.internal files path))]))]
    (reify
      Disposable
      (dispose [_]
        (run! Disposable/.dispose (vals sounds)))

      cdq.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (Sound/.play (get sounds path))))))
