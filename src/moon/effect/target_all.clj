(ns moon.effect.target-all
  "🚧 Under construction ⚠️"
  (:require [gdl.graphics.shape-drawer :as sd]
            [moon.effect :refer [source target]]
            [moon.player :as player]
            [moon.world.entities :as entities]
            [moon.world.line-of-sight :refer [line-of-sight?]]))

; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
; same code as in render entities on world view screens/world
(defn- creatures-in-los-of-player []
  (->> (entities/active)
       (filter #(:creature/species @%))
       (filter #(line-of-sight? @player/eid @%))
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

(defn info [_]
  "[LIGHT_GRAY]All visible targets[]")

(defn applicable? [_]
  true)

(defn useful? [_]
  ; TODO
  false
  )

(defn handle [[_ {:keys [entity-effects]}]]
  (let [source* @source]
    (apply concat
           (for [target (creatures-in-los-of-player)]
             [[:tx/line-render {:start (:position source*) #_(start-point source* target*)
                                :end (:position @target)
                                :duration 0.05
                                :color [1 0 0 0.75]
                                :thick? true}]
              ; some sound .... or repeat smae sound???
              ; skill do sound  / skill start sound >?
              ; problem : nested tx/effect , we are still having direction/target-position
              ; at sub-effects
              ; and no more safe - merge
              ; find a way to pass ctx / effect-ctx separate ?
              [:tx/effect {:effect/source source :effect/target target} entity-effects]]))))

(defn render [_]
  (let [source* @source]
    (doseq [target* (map deref (creatures-in-los-of-player))]
      (sd/line (:position source*) #_(start-point source* target*)
               (:position target*)
               [1 0 0 0.5]))))
