(ns forge.effects.target-all
  (:require [anvil.effect :refer [effects-do!]]
            [anvil.graphics :as g]
            [anvil.world :refer [player-eid active-entities line-of-sight? spawn-line-render]]))

; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
; same code as in render entities on world view screens/world
(defn- creatures-in-los-of-player []
  (->> (active-entities)
       (filter #(:entity/species @%))
       (filter #(line-of-sight? @player-eid @%))
       (remove #(:entity/player? @%))))

; TODO targets projectiles with -50% hp !!

(comment
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (creatures-in-los-of-player)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (:position @%)) targets)))

 )

(defn applicable? [_ _]
  true)

(defn useful? [_ _]
  ; TODO
  false
  )

(defn handle [[_ {:keys [entity-effects]}] {:keys [effect/source]}]
  (let [source* @source]
    (doseq [target (creatures-in-los-of-player)]
      (spawn-line-render {:start (:position source*) #_(start-point source* target*)
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
      (effects-do! {:effect/source source :effect/target target}
                   entity-effects))))

(defn render-effect [_ {:keys [effect/source]}]
  (let [source* @source]
    (doseq [target* (map deref (creatures-in-los-of-player))]
      (g/line (:position source*) #_(start-point source* target*)
              (:position target*)
              [1 0 0 0.5]))))
