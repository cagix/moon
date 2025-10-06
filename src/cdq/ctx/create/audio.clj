(ns cdq.ctx.create.audio
  (:require [cdq.audio]
            [clojure.edn :as edn]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.audio.sound :as sound]
            [clojure.java.io :as io]
            [clojure.disposable :as disposable]
            [clojure.files :as files]))

(def ^:private sound-names (->> "sounds.edn" io/resource slurp edn/read-string))
(def ^:private path-format "sounds/%s.wav")

(defn- audio-impl [audio sound-names->file-handles]
  (let [sounds (update-vals sound-names->file-handles
                            (fn [file-handle]
                              (audio/sound audio file-handle)))]
    (reify
      cdq.audio/Audio
      (sound-names [_]
        (map first sounds))

      (play! [_ sound-name]
        (assert (contains? sounds sound-name) (str sound-name))
        (sound/play! (get sounds sound-name)))

      disposable/Disposable
      (dispose! [_]
        (run! disposable/dispose! (vals sounds))))))

(defn- create
  [{:keys [clojure.gdx/audio
           clojure.gdx/files]}
   sound-names path-format]
  (let [sound-names->file-handles
        (into {}
              (for [sound-name sound-names]
                [sound-name
                 (files/internal files (format path-format sound-name))]))]
    (audio-impl audio sound-names->file-handles)))

(defn do! [{:keys [ctx/gdx]
            :as ctx}]
  (assoc ctx :ctx/audio (create gdx sound-names path-format)))
