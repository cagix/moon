(ns cdq.effects.target-all
  (:require [cdq.world.entity :as entity]
            [cdq.raycaster :as raycaster]))

(defn- creatures-in-los-of
  [{:keys [ctx/active-entities
           ctx/raycaster]}
   entity]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(raycaster/line-of-sight? raycaster entity @%))
       (remove #(:entity/player? @%))))

; TODO targets projectiles with -50% hp !!
(defn applicable? [_ _]
  true)

(defn useful? [_ _effect-ctx _ctx]
  ; TODO
  false)

(defn handle [[_ {:keys [entity-effects]}] {:keys [effect/source]} ctx]
  (let [source* @source]
    (apply concat
           (for [target (creatures-in-los-of ctx source*)]
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

(defn render [_ {:keys [effect/source]} ctx]
  (let [source* @source]
    (for [target* (map deref (creatures-in-los-of ctx source*))]
      [:draw/line
       (entity/position source*) #_(start-point source* target*)
       (entity/position target*)
       [1 0 0 0.5]])))
