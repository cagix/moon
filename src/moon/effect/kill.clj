(ns ^:no-doc moon.effect.kill
  (:require [moon.component :refer [defc] :as component]
            [moon.effect :as effect :refer [target]]))

(defc :effect.entity/kill
  {:schema :some}
  (component/info [_] "Kills target")

  (effect/applicable? [_]
    (and target (:entity/state @target)))

  (component/handle [_]
    [[:tx/event target :kill]]))
