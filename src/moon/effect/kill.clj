(ns moon.effect.kill
  (:require [moon.component :refer [defc] :as component]
            [moon.effect :refer [target]]))

(defc :effect.entity/kill
  {:schema :some}
  (component/info [_]
    "Kills target")

  (component/applicable? [_]
    (and target (:entity/fsm @target)))

  (component/handle [_]
    [[:tx/event target :kill]]))
