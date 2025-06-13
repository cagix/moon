(ns gdl.create.audio
  (:require [gdl.audio]
            [gdl.files :as files]
            [gdl.utils.assets :as assets]
            [gdl.utils.disposable :refer [dispose!]]
            [gdx.audio :as audio]
            [gdx.audio.sound :as sound]))

(defn do!
  [{:keys [ctx/audio
           ctx/files]}
   {:keys [sounds]}]
  (let [{:keys [folder extensions]} sounds
        sounds (into {}
                     (for [path (assets/search (files/internal files folder) extensions)]
                       [path (audio/new-sound audio (files/internal files path))]))]
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
