(ns moon.effect.convert
  (:require [moon.component :as component]
            [moon.entity :as entity]
            [moon.effect :refer [source target]]))

(defc :effect.entity/convert
  {:schema :some}
  (component/info [_]
    "Converts target to your side.")

  (component/applicable? [_]
    (and target
         (= (:entity/faction @target)
            (entity/enemy @source))))

  (component/handle [_]
    [[:tx/audiovisual (:position @target) :audiovisuals/convert]
     [:e/assoc target :entity/faction (entity/friend @source)]]))
