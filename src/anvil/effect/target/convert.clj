(ns ^:no-doc anvil.effect.target.convert
  (:require [anvil.component :as component]
            [anvil.entity :as entity]))

(defmethods :effects.target/convert
  (component/info [_]
    "Converts target to your side.")

  (component/applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (entity/enemy @source))))

  (component/handle [_ {:keys [effect/source effect/target]}]
    (swap! target assoc :entity/faction (:entity/faction @source))))
