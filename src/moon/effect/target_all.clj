(ns moon.effect.target-all
  (:require [moon.component :as component]
            [moon.effect :refer [source target]]
            [moon.entity.player :as player]
            [moon.graphics.shape-drawer :as sd]
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

(defc :effect/target-all
  {:schema [:s/map [:entity-effects]]
   :let {:keys [entity-effects]}}
  (component/info [_]
    "[LIGHT_GRAY]All visible targets[]")

  (component/applicable? [_]
    true)

  (component/useful? [_]
    ; TODO
    false
    )

  (component/handle [_]
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

  (component/render [_]
    (let [source* @source]
      (doseq [target* (map deref (creatures-in-los-of-player))]
        (sd/line (:position source*) #_(start-point source* target*)
                 (:position target*)
                 [1 0 0 0.5])))))
