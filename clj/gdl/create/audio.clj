(ns gdl.create.audio
  (:require [clojure.gdx.audio :as audio]
            [clojure.gdx.audio.sound :as sound]
            [clojure.utils.disposable :refer [dispose!]]
            [gdl.audio]))

(defn do!
  [{:keys [ctx/audio
           ctx/files]}
   {:keys [sounds]}]
  (let [sounds (into {}
                     (for [[path file-handle] (let [[f params] sounds]
                                                (f files params))]
                       [path (audio/new-sound audio file-handle)]))]
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
