(ns cdq.effects.target-all
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.utils :refer [defmethods]]
            [cdq.w :as w]))

(defmethods :effects/target-all
  ; TODO targets projectiles with -50% hp !!
  (effect/applicable? [_ _]
    true)

  (effect/useful? [_ _effect-ctx _world]
    ; TODO
    false)

  (effect/handle [[_ {:keys [entity-effects]}] {:keys [effect/source]} world]
    (let [source* @source]
      (apply concat
             (for [target (w/creatures-in-los-of-player world)]
               [[:tx/spawn-line {:start (entity/position source*) #_(start-point source* target*)
                                 :end (entity/position @target)
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

  (effect/render [_ {:keys [effect/source]} {:keys [ctx/world]}]
    (let [source* @source]
      (for [target* (map deref (w/creatures-in-los-of-player world))]
        [:draw/line
         (entity/position source*) #_(start-point source* target*)
         (entity/position target*)
         [1 0 0 0.5]]))))
