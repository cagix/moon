(ns gdl.create.audio
  (:require [gdl.assets :as assets]
            [gdl.audio :as audio]
            [gdl.fs :as fs]
            [gdl.utils.disposable :as disposable]))

(defn do! [{:keys [ctx/gdl]} {:keys [sounds]}]
  (let [sounds (into {}
                     (for [file (assets/find-assets (update sounds :folder (partial fs/internal gdl)))]
                       [file (audio/sound gdl file)]))]
    (reify
      disposable/Disposable
      (dispose! [_]
        (do
         ;(println "Disposing sounds ...")
         (run! disposable/dispose! (vals sounds))))

      gdl.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ path]
        (assert (contains? sounds path) (str path))
        (audio/play! (get sounds path))))))
