(ns moon.effect.stun
  (:require [moon.component :refer [defc] :as component]
            [gdl.utils :refer [readable-number]]
            [moon.effect :refer [target]]))

(defc :effect.entity/stun
  {:schema pos?
   :let duration}
  (component/info [_]
    (str "Stuns for " (readable-number duration) " seconds"))

  (component/applicable? [_]
    (and target (:entity/fsm @target)))

  (component/handle [_]
    [[:tx/event target :stun duration]]))
