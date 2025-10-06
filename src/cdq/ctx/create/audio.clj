(ns cdq.ctx.create.audio
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.badlogic.gdx.audio :as audio]
            [com.badlogic.gdx.files :as files]))

(def ^:private sound-names (->> "sounds.edn" io/resource slurp edn/read-string))
(def ^:private path-format "sounds/%s.wav")

(defn- audio-impl [{:keys [clojure.gdx/audio
                           clojure.gdx/files]}]
  (into {}
        (for [sound-name sound-names]
          [sound-name
           (->> sound-name
                (format path-format)
                (files/internal files)
                (audio/sound audio))])))

(defn do! [{:keys [ctx/gdx]
            :as ctx}]
  (assoc ctx :ctx/audio (audio-impl gdx)))
