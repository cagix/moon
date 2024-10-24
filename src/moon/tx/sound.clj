(ns moon.tx.sound
  (:require [moon.component :refer [defc]]
            [moon.assets :as assets]
            [moon.schema :as schema]
            [moon.tx :as tx]))

(defc :tx/sound
  {:schema :s/sound}
  (tx/handle [[_ file]]
    (assets/play-sound! file)
    nil))

(defmethod schema/form :s/sound [_] :string)
