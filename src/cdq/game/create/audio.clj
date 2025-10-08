(ns cdq.game.create.audio
  (:require [cdq.audio :as audio]))

(defn do!
  [{:keys [ctx/gdx]
    :as ctx}
   sound-names
   path-format]
  (assoc ctx :ctx/audio (audio/create gdx sound-names path-format)))
