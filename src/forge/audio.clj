(ns forge.audio
  (:require [forge.assets :as assets])
  (:import (com.badlogic.gdx.audio Sound)))

(defn play-sound [name]
  (Sound/.play (get assets/manager (str "sounds/" name ".wav"))))
