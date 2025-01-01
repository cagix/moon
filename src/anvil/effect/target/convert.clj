(ns ^:no-doc anvil.effect.target.convert
  (:require [anvil.entity :as entity]
            [clojure.component :as component :refer [defcomponent]]))

(defcomponent :effects.target/convert
  (component/segment [_ _c]
    "Converts target to your side.")

  (component/applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (entity/enemy @source))))

  (component/handle [_ {:keys [effect/source effect/target]} c]
    (swap! target assoc :entity/faction (:entity/faction @source))))
