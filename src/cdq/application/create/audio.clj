(ns cdq.application.create.audio
  (:require [cdq.audio]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.audio :as audio]
            [gdl.audio.sound :as sound]
            [gdl.files :as files]
            [gdl.utils.disposable :as disposable]))

(def ^:private sound-names (->> "sounds.edn" io/resource slurp edn/read-string))
(def ^:private path-format "sounds/%s.wav")

(defn- audio-impl [audio sound-names->file-handles]
  (let [sounds (update-vals sound-names->file-handles
                            (fn [file-handle]
                              (audio/new-sound audio file-handle)))]
    (reify
      cdq.audio/Audio
      (all-sounds [_]
        (map first sounds))

      (play-sound! [_ sound-name]
        (assert (contains? sounds sound-name) (str sound-name))
        (sound/play! (get sounds sound-name)))

      disposable/Disposable
      (dispose! [_]
        (run! disposable/dispose! (vals sounds))))))

(defn do!
  [{:keys [ctx/audio
           ctx/files]
    :as ctx}]
  (assoc ctx :ctx/audio (let [sound-names->file-handles
                              (into {}
                                    (for [sound-name sound-names]
                                      [sound-name
                                       (files/internal files (format path-format sound-name))]))]
                          (audio-impl audio sound-names->file-handles))))
