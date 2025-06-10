(ns gdl.create.audio
  (:require [gdl.assets :as assets]
            [gdl.audio :as audio]
            [gdl.fs :as fs]
            [gdl.utils.disposable])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.utils Disposable)))

(extend-type Disposable
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(defn do! [{:keys [ctx/gdl]} {:keys [sounds]}]
  (let [sounds (into {}
                     (for [file (assets/find-assets (update sounds :folder (partial fs/internal gdl)))]
                       [file (audio/sound gdl file)]))]
    (reify
      gdl.utils.disposable/Disposable
      (dispose! [_]
        (do
         ;(println "Disposing sounds ...")
         (run! Disposable/.dispose (vals sounds))))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (Sound/.play (get sounds path))))))
