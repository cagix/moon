(ns cdq.audio.sound
  (:require [cdq.ctx :as ctx])
  (:import (com.badlogic.gdx.audio Sound)))

(defn play! [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")
       ctx/assets
       Sound/.play))
