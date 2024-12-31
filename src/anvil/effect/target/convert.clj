(ns ^:no-doc anvil.effect.target.convert
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [gdl.info :as info]
            [clojure.utils :refer [defmethods]]))

(defmethods :effects.target/convert
  (info/segment [_ _c]
    "Converts target to your side.")

  (component/applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (entity/enemy @source))))

  (component/handle [_ {:keys [effect/source effect/target]} c]
    (swap! target assoc :entity/faction (:entity/faction @source))))
