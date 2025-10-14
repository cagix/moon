(ns cdq.game.create.get-gdx
  (:require [cdq.audio :as audio]
            [cdq.impl.graphics]))

(defn do!
  [{:keys [audio
           files
           graphics
           input]}
   config]
  {:ctx/audio (audio/create audio files (:audio config))
   :ctx/graphics (cdq.impl.graphics/create! graphics files (:graphics config))
   :ctx/input input})
