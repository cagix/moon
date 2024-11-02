(ns moon.effect.entity.convert
  (:require [moon.component :as component]
            [moon.effect :refer [source target]]
            [moon.faction :as faction]))

(defmethods :effect.entity/convert
  (component/info [_]
    "Converts target to your side.")

  (component/applicable? [_]
    (and target
         (= (:entity/faction @target)
            (faction/enemy @source))))

  (component/handle [_]
    [[:tx/audiovisual (:position @target) :audiovisuals/convert]
     [:e/assoc target :entity/faction (:entity/faction @source)]]))
