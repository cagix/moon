(ns gdl.create.audio
  (:require [clojure.gdx :as gdx]
            [gdl.audio :as audio]
            [gdl.files :as files]
            [gdl.utils.disposable]))

(defn do!
  [{:keys [ctx/audio
           ctx/files]}
   {:keys [sounds]}]
  (let [{:keys [folder extensions]} sounds
        sounds (into {}
                     (for [path (gdx/find-assets (files/internal files folder) extensions)]
                       [path (.newSound audio (files/internal files path))]))]
    (reify
      gdl.utils.disposable/Disposable
      (dispose! [_]
        (run! gdl.utils.disposable/dispose! (vals sounds)))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (gdx/play! (get sounds path))))))
