(ns cdq.game.create.audio
  (:require [cdq.audio]
            [clojure.audio :as audio]
            [clojure.sound :as sound]))

(deftype Audio [sounds]
  cdq.audio/Audio
  (sound-names [_]
    (map first sounds))

  (play! [_ sound-name]
    (assert (contains? sounds sound-name) (str sound-name))
    (sound/play! (get sounds sound-name)))

  (dispose! [_]
    (run! sound/dispose! (vals sounds))))

(defn- create-impl [gdx {:keys [sound-names path-format]}]
  (->Audio
   (into {}
         (for [sound-name sound-names]
           [sound-name
            (->> sound-name
                 (format path-format)
                 (audio/sound gdx))]))) )

(defn do!
  [{:keys [ctx/gdx]
    :as ctx}
   params]
  (assoc ctx :ctx/audio (create-impl gdx params)))
