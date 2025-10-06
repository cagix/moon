(ns cdq.ctx.create.audio
  (:require [cdq.audio]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.java.io :as io]
            [clojure.disposable :as disposable]))

(def ^:private sound-names (->> "sounds.edn" io/resource slurp edn/read-string))
(def ^:private path-format "sounds/%s.wav")

(defn- audio-impl [gdx]
  (let [sounds (into {}
                     (for [sound-name sound-names]
                       [sound-name
                        (gdx/sound gdx (format path-format sound-name))]))]
    (reify
      cdq.audio/Audio
      (sound-names [_]
        (map first sounds))

      (play! [_ sound-name]
        (assert (contains? sounds sound-name) (str sound-name))
        (audio/play! (get sounds sound-name)))

      disposable/Disposable
      (dispose! [_]
        (run! disposable/dispose! (vals sounds))))))

(defn do! [{:keys [ctx/gdx]
            :as ctx}]
  (assoc ctx :ctx/audio (audio-impl gdx)))
