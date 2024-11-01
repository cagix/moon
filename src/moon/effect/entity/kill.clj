(ns moon.effect.entity.kill
  (:require [moon.component :as component]
            [moon.effect :refer [target]]))

(defc :effect.entity/kill
  {:schema :some}
  (component/info [_]
    "Kills target")

  (component/applicable? [_]
    (and target (:entity/fsm @target)))

  (component/handle [_]
    [[:entity/fsm target :kill]]))
