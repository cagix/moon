(ns moon.tx.sound
  (:require [moon.component :as component]
            [moon.assets :as assets]))

(defmethods :tx/sound
  (component/handle [[_ file]]
    (assets/play-sound! file)
    nil))
