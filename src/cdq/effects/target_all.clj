(ns cdq.effects.target-all
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.effect :as effect]
            [cdq.utils :refer [defcomponent]]
            [cdq.world :as world]))

(defn- creatures-in-los-of-player []
  (->> ctx/active-entities
       (filter #(:entity/species @%))
       (filter #(world/line-of-sight? ctx/world @ctx/player-eid @%))
       (remove #(:entity/player? @%))))

(defcomponent :effects/target-all
  ; TODO targets projectiles with -50% hp !!
  (effect/applicable? [_ _]
    true)

  (effect/useful? [_ _]
    ; TODO
    false)

  (effect/handle [[_ {:keys [entity-effects]}] {:keys [effect/source]}]
    (let [source* @source]
      (apply concat
             (for [target (creatures-in-los-of-player)]
               [[:tx/spawn-line {:start (:position source*) #_(start-point source* target*)
                                 :end (:position @target)
                                 :duration 0.05
                                 :color [1 0 0 0.75]
                                 :thick? true}]
                ; some sound .... or repeat smae sound???
                ; skill do sound  / skill start sound >?
                ; problem : nested effect/do-all! , we are still having direction/target-position
                ; at sub-effects
                ; and no more safe - merge
                ; find a way to pass ctx / effect-ctx separate ?
                [:tx/effect {:effect/source source :effect/target target} entity-effects]]))))

  (effect/render [_ {:keys [effect/source]}]
    (let [source* @source]
      (doseq [target* (map deref (creatures-in-los-of-player))]
        (draw/line (:position source*) #_(start-point source* target*)
                   (:position target*)
                   [1 0 0 0.5])))))
