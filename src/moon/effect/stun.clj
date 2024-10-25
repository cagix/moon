(ns moon.effect.stun
  (:require [moon.component :refer [defc] :as component]
            [gdl.utils :refer [readable-number]]
            [moon.effect :as effect :refer [target]]) )

(defc :effect.entity/stun
  {:schema pos?
   :let duration}
  (component/info [_]
    (str "Stuns for " (readable-number duration) " seconds"))

  (effect/applicable? [_]
    (and target (:entity/state @target)))

  (component/handle [_]
    [[:tx/event target :stun duration]]))
