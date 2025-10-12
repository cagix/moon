(ns cdq.game.create.get-gdx
  (:require [cdq.audio]
            [cdq.impl.graphics]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio.sound :as sound]))

(defn- create-audio
  [sounds]
  (reify cdq.audio/Audio
    (sound-names [_]
      (map first sounds))

    (play! [_ sound-name]
      (assert (contains? sounds sound-name) (str sound-name))
      (sound/play! (get sounds sound-name)))

    (dispose! [_]
      (run! sound/dispose! (vals sounds)))))

(defn do! [ctx config]
  (let [{:keys [audio
                files
                graphics
                input]} (gdx/context)
        {:keys [sound-names path-format]} (:audio config)
        sound-name->file-handle (into {}
                                      (for [sound-name sound-names
                                            :let [path (format path-format sound-name)]]
                                        [sound-name
                                         (.internal files path)]))
        sounds (into {}
                     (for [[sound-name file-handle] sound-name->file-handle]
                       [sound-name
                        (.newSound audio file-handle)]))
        ]
    (assoc ctx
           :ctx/audio (create-audio sounds)
           :ctx/graphics (cdq.impl.graphics/create! graphics files (:graphics config))
           :ctx/input input)))
