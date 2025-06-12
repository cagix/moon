(ns gdl.create.audio
  (:require [clojure.gdx :as gdx]
            [gdl.audio :as audio]
            [gdl.utils.disposable]))

(defn do! [_ctx {:keys [sounds]}]
  (let [sounds (into {}
                     (for [path (gdx/find-assets sounds)]
                       [path (gdx/load-sound path)]))]
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
