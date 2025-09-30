(ns cdq.audio
  (:require [com.badlogic.gdx.audio :as audio]
            [com.badlogic.gdx.audio.sound :as sound]
            [com.badlogic.gdx.files :as files]
            [com.badlogic.gdx.utils.disposable :as disposable]))

(defprotocol Audio
  (all-sounds [_])
  (play-sound! [_ sound-name]))

(defn- audio-impl [audio sound-names->file-handles]
  (let [sounds (update-vals sound-names->file-handles
                            (fn [file-handle]
                              (audio/new-sound audio file-handle)))]
    (reify
      Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ sound-name]
        (assert (contains? sounds sound-name) (str sound-name))
        (sound/play! (get sounds sound-name)))

      disposable/Disposable
      (dispose! [_]
        (run! disposable/dispose! (vals sounds))))))

(defn create
  [audio files sound-names path-format]
  (let [sound-names->file-handles
        (into {}
              (for [sound-name sound-names]
                [sound-name
                 (files/internal files (format path-format sound-name))]))]
    (audio-impl audio sound-names->file-handles)))
