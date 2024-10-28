(ns moon.tx.sound
  (:require [moon.component :as component]
            [moon.assets :as assets]))

(defc :tx/sound
  {:schema :s/sound}
  (component/handle [[_ file]]
    (assets/play-sound! file)
    nil))
