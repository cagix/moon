(ns cdq.game.create.audio
  (:require [cdq.audio]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]))

(deftype Audio [sounds]
  cdq.audio/Audio
  (sound-names [_]
    (map first sounds))

  (play! [_ sound-name]
    (assert (contains? sounds sound-name) (str sound-name))
    (audio/play! (get sounds sound-name)))

  (dispose! [_]
    (run! audio/dispose! (vals sounds))))

(defn- create-impl [gdx {:keys [sound-names path-format]}]
  (->Audio
   (into {}
         (for [sound-name sound-names]
           [sound-name
            (->> sound-name
                 (format path-format)
                 (gdx/sound gdx))]))) )

(defn do!
  [{:keys [ctx/gdx]
    :as ctx}
   params]
  (assoc ctx :ctx/audio (create-impl gdx params)))
