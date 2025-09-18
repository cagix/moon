(ns cdq.application.render
  (:require [clojure.utils :as utils]
            [gdl.scene2d.stage :as stage]))

(defn do! [state pipeline]
  (swap! state utils/pipeline pipeline)
  (stage/act!  (:ctx/stage @state))
  (stage/draw! (:ctx/stage @state)))
