(ns moon.tx.sound
  (:require [moon.component :refer [defc] :as component]
            [moon.assets :as assets]
            [moon.schema :as schema]))

(defc :tx/sound
  {:schema :s/sound}
  (component/handle [[_ file]]
    (assets/play-sound! file)
    nil))

(defmethod schema/form :s/sound [_] :string)
