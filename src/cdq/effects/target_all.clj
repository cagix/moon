(ns cdq.effects.target-all
  (:require [cdq.world :as world]))

(defn affected-targets
  [active-entities
   world
   entity]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(world/line-of-sight? world entity @%))
       (remove #(:entity/player? @%))))

(comment
 ; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
 ; same code as in render entities on world view screens/world
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (creatures-in-los-of-player)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (entity/position @%)) targets)))

 )

; TODO targets projectiles with -50% hp !!
(defn applicable? [_ _]
  true)

; TODO
(defn useful? [_ _effect-ctx _ctx]
  false)

(defn handle
  [[_ {:keys [entity-effects]}]
   {:keys [effect/source]}
   {:keys [ctx/world]}]
  (let [{:keys [world/active-entities]} world
        source* @source]
    (apply concat
           (for [target (affected-targets active-entities world source*)]
             [[:tx/spawn-line
               {:start (:body/position (:entity/body source*)) #_(start-point source* target*)
                :end (:body/position (:entity/body @target))
                :duration 0.05
                :color [1 0 0 0.75]
                :thick? true}]
              [:tx/effect
               {:effect/source source
                :effect/target target}
               entity-effects]]))))
