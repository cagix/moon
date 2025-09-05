(ns cdq.effects.target-all
  (:require [cdq.raycaster :as raycaster]))

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

; TODO
(defn useful? [_ _effect-ctx _ctx]
  false)

(defn handle [[_ {:keys [entity-effects]}] {:keys [effect/source]} ctx]
  (let [source* @source]
    (apply concat
           (for [target (creatures-in-los-of ctx source*)]
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

(defn render [_ {:keys [effect/source]} ctx]
  (let [source* @source]
    (for [target* (map deref (creatures-in-los-of ctx source*))]
      [:draw/line
       (:body/position (:entity/body source*)) #_(start-point source* target*)
       (:body/position (:entity/body target*))
       [1 0 0 0.5]])))
