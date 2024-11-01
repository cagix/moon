(ns moon.effect.entity.convert
  (:require [moon.component :as component]
            [moon.effect :refer [source target]]
            [moon.entity.faction :as faction]))

(defc :effect.entity/convert
  {:schema :some}
  (component/info [_]
    "Converts target to your side.")

  (component/applicable? [_]
    (and target
         (= (:entity/faction @target)
            (faction/enemy @source))))

  (component/handle [_]
    [[:tx/audiovisual (:position @target) :audiovisuals/convert]
     [:e/assoc target :entity/faction (:entity/faction @source)]]))
