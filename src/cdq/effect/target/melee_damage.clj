(ns cdq.effect.target.melee-damage
  (:require [anvil.entity :as entity]
            [clojure.component :as component]))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

; FIXME no source
; => to entity move
(defn info [_ _c]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))

(defn applicable? [_ {:keys [effect/source] :as ctx}]
  (component/applicable? (damage-effect @source) ctx))

(defn handle [_ {:keys [effect/source] :as ctx} c]
  (component/handle (damage-effect @source) ctx c))
