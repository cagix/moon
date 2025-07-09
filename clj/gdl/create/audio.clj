(ns gdl.create.audio
  (:require [clojure.audio :as audio]
            [clojure.audio.sound :as sound]
            [clojure.edn :as edn]
            [clojure.files :as files]
            [clojure.java.io :as io]
            [clojure.utils.disposable :refer [dispose!]]
            [gdl.audio]))

(defn do!
  [{:keys [ctx/audio
           ctx/files]}
   {:keys [sounds]}]
  (let [sounds (into {}
                     (for [path (->> sounds io/resource slurp edn/read-string)]
                       [path (audio/new-sound audio (files/internal files path))]))]
    (reify
      clojure.utils.disposable/Disposable
      (dispose! [_]
        (run! dispose! (vals sounds)))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (sound/play! (get sounds path))))))
