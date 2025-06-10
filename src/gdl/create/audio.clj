(ns gdl.create.audio
  (:require [gdl.assets :as assets]
            [gdl.audio]
            [gdl.utils.disposable])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.utils Disposable)))

(extend-type Disposable
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(defn do! [_ctx {:keys [sounds]}]
  (let [sounds (into {}
                     (for [file (assets/find-assets sounds)]
                       [file (.newSound Gdx/audio (.internal Gdx/files file))]))]
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
