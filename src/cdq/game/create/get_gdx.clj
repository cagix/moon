(ns cdq.game.create.get-gdx
  (:require [cdq.audio]
            [cdq.impl.graphics]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.files :as files]))

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

(defn do!
  [{:keys [audio
           files
           graphics
           input]}
   config]
  (let [{:keys [sound-names path-format]} (:audio config)
        sound-name->file-handle (into {}
                                      (for [sound-name sound-names
                                            :let [path (format path-format sound-name)]]
                                        [sound-name
                                         (files/internal files path)]))
        sounds (into {}
                     (for [[sound-name file-handle] sound-name->file-handle]
                       [sound-name
                        (audio/sound audio file-handle)]))]
    {:ctx/audio (create-audio sounds)
     :ctx/graphics (cdq.impl.graphics/create! graphics files (:graphics config))
     :ctx/input input}))
