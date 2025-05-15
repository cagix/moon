(ns cdq.tx.sound
  (:require [cdq.ctx :as ctx])
  (:import (com.badlogic.gdx.audio Sound)))

(defn do! [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       ctx/assets
       Sound/.play))
