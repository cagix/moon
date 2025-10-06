(ns cdq.ctx.create.audio
  (:require [cdq.audio]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.disposable :as disposable]
            [com.badlogic.gdx.audio :as audio]
            [com.badlogic.gdx.audio.sound :as sound]
            [com.badlogic.gdx.files :as files]))

(def ^:private sound-names (->> "sounds.edn" io/resource slurp edn/read-string))
(def ^:private path-format "sounds/%s.wav")

(defn- audio-impl [{:keys [clojure.gdx/audio
                           clojure.gdx/files]}]
  (let [sounds (into {}
                     (for [sound-name sound-names]
                       [sound-name
                        (->> sound-name
                             (format path-format)
                             (files/internal files)
                             (audio/sound audio))]))]
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

(defn do! [{:keys [ctx/gdx]
            :as ctx}]
  (assoc ctx :ctx/audio (audio-impl gdx)))
