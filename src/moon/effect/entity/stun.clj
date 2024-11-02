(ns moon.effect.entity.stun
  (:require [moon.component :as component]
            [gdl.utils :refer [readable-number]]
            [moon.effect :refer [target]]))

(defmethods :effect.entity/stun
  {:let duration}
  (component/info [_]
    (str "Stuns for " (readable-number duration) " seconds"))

  (component/applicable? [_]
    (and target (:entity/fsm @target)))

  (component/handle [_]
    [[:entity/fsm target :stun duration]]))
