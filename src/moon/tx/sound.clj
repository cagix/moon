(ns moon.tx.sound
  (:require [moon.component :as component]
            [moon.assets :as assets]))

(defc :tx/sound
  (component/handle [[_ file]]
    (assets/play-sound! file)
    nil))
