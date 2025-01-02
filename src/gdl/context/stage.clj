(ns gdl.context.stage
  (:require [clojure.gdx :as gdx]
            [gdl.ui :as ui]))

(defn create [_ {:keys [gdl.context/viewport
                        gdl.context/batch] :as c}]
  (let [stage (ui/stage viewport batch nil)]
    (gdx/set-input-processor c stage)
    stage))

(defn dispose [[_ stage]]
  (gdx/dispose stage))
