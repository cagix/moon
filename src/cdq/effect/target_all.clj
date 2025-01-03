(ns cdq.effect.target-all
  (:require [cdq.context :as world]
            [cdq.effect-context :refer [do-all!]]
            [gdl.context :as c]))

(comment
 ; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
 ; same code as in render entities on world view screens/world
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (world/creatures-in-los-of-player)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (:position @%)) targets)))

 )

; TODO targets projectiles with -50% hp !!
(defn info [_ _entity _c]
  "All visible targets")

(defn applicable? [_ _]
  true)

(defn useful? [_ _ _c]
  ; TODO
  false)

(defn handle [[_ {:keys [entity-effects]}] {:keys [effect/source]} c]
  (let [source* @source]
    (doseq [target (world/creatures-in-los-of-player c)]
      (world/line-render c
                         {:start (:position source*) #_(start-point source* target*)
                          :end (:position @target)
                          :duration 0.05
                          :color [1 0 0 0.75]
                          :thick? true})
      ; some sound .... or repeat smae sound???
      ; skill do sound  / skill start sound >?
      ; problem : nested tx/effect , we are still having direction/target-position
      ; at sub-effects
      ; and no more safe - merge
      ; find a way to pass ctx / effect-ctx separate ?
      (do-all! c
               {:effect/source source :effect/target target}
               entity-effects))))

(defn render [_ {:keys [effect/source]} c]
  (let [source* @source]
    (doseq [target* (map deref (world/creatures-in-los-of-player c))]
      (c/line c
              (:position source*) #_(start-point source* target*)
              (:position target*)
              [1 0 0 0.5]))))
