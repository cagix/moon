(ns moon.tx.sound
  (:require [gdl.assets :as assets]
            [moon.component :as component]))

(defmethods :tx/sound
  (component/handle [[_ file]]
    (assets/play-sound file)
    nil))
