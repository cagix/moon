(ns cdq.effects.target-all
  (:require [cdq.effect :as effect]
            [cdq.g :as g]
            [cdq.utils :refer [defcomponent]]))

(defn- creatures-in-los-of-player [{:keys [ctx/active-entities
                                           ctx/player-eid]
                                    :as ctx}]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(g/line-of-sight? ctx @player-eid @%))
       (remove #(:entity/player? @%))))

(defcomponent :effects/target-all
  ; TODO targets projectiles with -50% hp !!
  (effect/applicable? [_ _]
    true)

  (effect/useful? [_ _effect-ctx _ctx]
    ; TODO
    false)

  (effect/handle [[_ {:keys [entity-effects]}] {:keys [effect/source]} ctx]
    (let [source* @source]
      (apply concat
             (for [target (creatures-in-los-of-player ctx)]
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

  (effect/render [_ {:keys [effect/source]} ctx]
    (let [source* @source]
      (for [target* (map deref (creatures-in-los-of-player ctx))]
        [:draw/line
         (:position source*) #_(start-point source* target*)
         (:position target*)
         [1 0 0 0.5]]))))
