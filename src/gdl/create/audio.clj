(ns gdl.create.audio
  (:require [gdl.audio]
            [gdl.utils.disposable :refer [dispose!]]
            [gdx.audio :as audio]
            [gdx.audio.sound :as sound]))

(defn do!
  [{:keys [ctx/audio] :as ctx}
   {:keys [sounds]}]
  (let [sounds (into {}
                     (for [[path file-handle] (let [[f params] sounds]
                                                (f ctx params))]
                       [path (audio/new-sound audio file-handle)]))]
    (reify
      gdl.utils.disposable/Disposable
      (dispose! [_]
        (run! dispose! (vals sounds)))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (sound/play! (get sounds path))))))
